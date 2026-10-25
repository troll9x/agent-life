package vn.lifeos.data.network

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import vn.lifeos.domain.common.SourceRef
import vn.lifeos.domain.common.SourceKind
import vn.lifeos.domain.common.SourceState
import vn.lifeos.domain.common.SourceStatus
import vn.lifeos.domain.integration.AccessTokenProvider
import vn.lifeos.domain.integration.ConnectionFailure
import vn.lifeos.domain.integration.ConnectionResult
import vn.lifeos.domain.integration.connectionFailure
import java.io.IOException
import java.io.ByteArrayOutputStream
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.Clock
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Only structural event codes leave this layer. Never pass URL, headers, body or exception text. */
fun interface NetworkEventSink {
    fun record(provider: SourceKind, event: NetworkEvent)
}

enum class NetworkEvent { SUCCESS, AUTH_REQUIRED, PERMISSION_DENIED, RATE_LIMITED, OFFLINE, TIMED_OUT, PROVIDER_ERROR, INVALID_RESPONSE, RESPONSE_TOO_LARGE, REDIRECT_BLOCKED }

/** Explicit endpoint contract. Cleartext is accepted only on loopback for local fixtures. */
class SourceEndpoint(baseUrl: String) {
    val base: HttpUrl = baseUrl.toHttpUrl().also {
        require(it.scheme == "https" || (it.scheme == "http" && it.host in setOf("localhost", "127.0.0.1")))
        require(it.username.isEmpty() && it.password.isEmpty())
        require(it.query == null && it.fragment == null)
    }

    fun resolve(pathOrUrl: String): HttpUrl? = base.resolve(pathOrUrl)?.takeIf(::accepts)

    fun accepts(url: HttpUrl): Boolean = url.scheme == base.scheme && url.host == base.host && url.port == base.port &&
        url.username.isEmpty() && url.password.isEmpty()
}

sealed interface PostRetryContract {
    data object None : PostRetryContract
    /** Connector may use this only when the provider documents idempotency for this endpoint/key. */
    data class ProviderIdempotencyKey(val value: String) : PostRetryContract {
        init { require(value.matches(Regex("[A-Za-z0-9_-]{8,128}"))) }
    }
}

data class SourceRequest(
    val source: SourceRef,
    val endpoint: SourceEndpoint,
    val pathOrUrl: String,
    val authenticated: Boolean = true,
    val method: Method = Method.GET,
    val jsonBody: String? = null,
    val postRetryContract: PostRetryContract = PostRetryContract.None,
) {
    enum class Method { GET, POST }
    init {
        require(method == Method.POST || jsonBody == null)
        require(method == Method.POST || postRetryContract == PostRetryContract.None)
        require(method == Method.GET || jsonBody != null)
    }
}

/** HTTP foundation; provider DTOs are decoded by connector-supplied serializers. */
class SourceHttpClient(
    private val tokens: AccessTokenProvider,
    private val clock: Clock = Clock.systemUTC(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val events: NetworkEventSink = NetworkEventSink { _, _ -> },
    private val maxResponseBytes: Int = 1_048_576,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build(),
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    init { require(maxResponseBytes > 0) }

    suspend fun <T> fetch(request: SourceRequest, serializer: KSerializer<T>): ConnectionResult<T> {
        var url = request.endpoint.resolve(request.pathOrUrl)
            ?: return fail(request.source, ConnectionFailure.REDIRECT_BLOCKED)
        var token = if (request.authenticated) tokens.tokenFor(request.source)
            ?: return fail(request.source, ConnectionFailure.AUTH_REQUIRED) else null
        var refreshed = false
        var redirects = 0
        var attempts = 0
        val retryable = request.method == SourceRequest.Method.GET || request.postRetryContract is PostRetryContract.ProviderIdempotencyKey
        val maxAttempts = if (retryable) 3 else 1

        while (true) {
            attempts++
            val raw = try {
                execute(request, url, token)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (timeout: SocketTimeoutException) {
                currentCoroutineContext().ensureActive()
                if (attempts < maxAttempts) { delay(backoff(attempts)); continue }
                return fail(request.source, ConnectionFailure.TIMED_OUT)
            } catch (offline: IOException) {
                currentCoroutineContext().ensureActive()
                if (attempts < maxAttempts && offline !is UnknownHostException && offline !is ConnectException) {
                    delay(backoff(attempts)); continue
                }
                return fail(request.source, ConnectionFailure.OFFLINE)
            }
            when (raw.code) {
                401 -> {
                    if (!request.authenticated || refreshed) return fail(request.source, ConnectionFailure.AUTH_REQUIRED)
                    token = tokens.refresh(request.source) ?: return fail(request.source, ConnectionFailure.AUTH_REQUIRED)
                    refreshed = true
                    attempts = 0
                }
                403 -> return fail(request.source, ConnectionFailure.PERMISSION_DENIED)
                429 -> return fail(request.source, ConnectionFailure.RATE_LIMITED, retryAt(raw.retryAfter))
                in 300..399 -> {
                    if (request.method == SourceRequest.Method.POST) return fail(request.source, ConnectionFailure.REDIRECT_BLOCKED)
                    val next = raw.location?.let(url::resolve)
                    if (next == null || !request.endpoint.accepts(next) || ++redirects > 3) {
                        return fail(request.source, ConnectionFailure.REDIRECT_BLOCKED)
                    }
                    url = next
                    attempts = 0
                }
                in 500..599 -> {
                    if (attempts < maxAttempts) { delay(backoff(attempts)); continue }
                    return fail(request.source, ConnectionFailure.PROVIDER_ERROR)
                }
                in 200..299 -> {
                    if (raw.tooLarge) return fail(request.source, ConnectionFailure.RESPONSE_TOO_LARGE)
                    if (raw.body == null) return fail(request.source, ConnectionFailure.INVALID_RESPONSE)
                    if (raw.contentType?.substringBefore(';')?.trim()?.lowercase() != "application/json") {
                        return fail(request.source, ConnectionFailure.INVALID_RESPONSE)
                    }
                    val value = try { json.decodeFromString(serializer, raw.body.decodeToString()) }
                    catch (_: SerializationException) { return fail(request.source, ConnectionFailure.INVALID_RESPONSE) }
                    catch (_: IllegalArgumentException) { return fail(request.source, ConnectionFailure.INVALID_RESPONSE) }
                    events.record(request.source.kind, NetworkEvent.SUCCESS)
                    return ConnectionResult.Success(value, SourceStatus(SourceState.OK, checkedAt = clock.instant(), lastSuccessfulAt = clock.instant()))
                }
                else -> return fail(request.source, ConnectionFailure.PROVIDER_ERROR)
            }
        }
    }

    /** Page traversal uses the same origin checks as redirects and rejects repeated cursors. */
    suspend fun <T> fetchPages(
        first: SourceRequest,
        serializer: KSerializer<T>,
        nextLink: (T) -> String?,
        maxPages: Int = 100,
    ): ConnectionResult<List<T>> {
        require(first.method == SourceRequest.Method.GET && maxPages > 0)
        val pages = mutableListOf<T>()
        val seen = mutableSetOf<String>()
        var path = first.pathOrUrl
        while (true) {
            val resolved = first.endpoint.resolve(path) ?: return fail(first.source, ConnectionFailure.REDIRECT_BLOCKED)
            if (!seen.add(resolved.toString()) || pages.size >= maxPages) return fail(first.source, ConnectionFailure.INVALID_RESPONSE)
            when (val result = fetch(first.copy(pathOrUrl = resolved.toString()), serializer)) {
                is ConnectionResult.Failure -> return result
                is ConnectionResult.Success -> {
                    pages += result.value
                    path = nextLink(result.value) ?: return ConnectionResult.Success(pages, result.status)
                }
            }
        }
    }

    private data class Raw(val code: Int, val body: ByteArray?, val contentType: String?, val retryAfter: String?, val location: String?, val tooLarge: Boolean)

    private suspend fun execute(request: SourceRequest, url: HttpUrl, token: String?): Raw = withContext(dispatcher) {
        val builder = Request.Builder().url(url).header("Accept", "application/json")
        if (token != null) builder.header("Authorization", "Bearer $token")
        if (request.postRetryContract is PostRetryContract.ProviderIdempotencyKey) {
            builder.header("Idempotency-Key", request.postRetryContract.value)
        }
        when (request.method) {
            SourceRequest.Method.GET -> builder.get()
            SourceRequest.Method.POST -> builder.post(request.jsonBody!!.toRequestBody("application/json; charset=utf-8".toMediaType()))
        }
        suspendCancellableCoroutine { continuation ->
            val call = client.newCall(builder.build())
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        val raw = response.use { readResponse(it) }
                        if (continuation.isActive) continuation.resume(raw)
                    } catch (e: IOException) {
                        if (continuation.isActive) continuation.resumeWithException(e)
                    }
                }
            })
        }
    }

    private fun readResponse(response: Response): Raw {
        val length = response.body.contentLength()
        val tooLarge = length > maxResponseBytes
        val bytes = if (!tooLarge && response.code in 200..299) {
            response.body.byteStream().use { stream ->
                val out = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (out.size() <= maxResponseBytes) {
                    val read = stream.read(buffer, 0, minOf(buffer.size, maxResponseBytes + 1 - out.size()))
                    if (read < 0) break
                    out.write(buffer, 0, read)
                }
                out.toByteArray()
            }
        } else null
        return Raw(response.code, bytes, response.header("Content-Type"), response.header("Retry-After"), response.header("Location"), tooLarge || (bytes?.size ?: 0) > maxResponseBytes)
    }

    private fun retryAt(header: String?): Instant? {
        val now = clock.instant()
        return header?.trim()?.toLongOrNull()?.takeIf { it >= 0 }?.let { now.plusSeconds(it.coerceAtMost(86_400)) }
            ?: try { header?.let { ZonedDateTime.parse(it, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().coerceAtLeast(now) } }
            catch (_: Exception) { null }
    }

    private fun backoff(attempt: Int): Long = (attempt * 200L).coerceAtMost(1_000L)

    private fun fail(source: SourceRef, reason: ConnectionFailure, retryAt: Instant? = null): ConnectionResult.Failure {
        events.record(source.kind, NetworkEvent.valueOf(reason.name))
        return connectionFailure(reason, clock.instant(), retryAt)
    }
}
