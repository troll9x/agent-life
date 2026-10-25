package vn.lifeos.data.network

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import vn.lifeos.domain.common.SourceKind
import vn.lifeos.domain.common.SourceRef
import vn.lifeos.domain.common.SourceState
import vn.lifeos.domain.integration.AccessTokenProvider
import vn.lifeos.domain.integration.ConnectionFailure
import vn.lifeos.domain.integration.ConnectionResult
import java.net.SocketTimeoutException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SourceHttpClientTest {
    @Serializable private data class Page(val items: List<Int>, val next: String? = null)
    private val source = SourceRef(SourceKind.MICROSOFT_MAIL, "fixture-source")
    private val clock = Clock.fixed(Instant.parse("2026-10-25T00:00:00Z"), ZoneOffset.UTC)
    private fun server() = MockWebServer().apply { start() }
    private val tokenProvider = object : AccessTokenProvider {
        var refreshes = 0
        override suspend fun tokenFor(source: SourceRef) = "fixture-secret-token"
        override suspend fun refresh(source: SourceRef): String? { refreshes++; return "fixture-refreshed-token" }
    }

    private fun request(server: MockWebServer, path: String = "/items") =
        SourceRequest(source, SourceEndpoint(server.url("/").toString()), path)

    private fun json(body: String) = MockResponse.Builder().code(200).addHeader("Content-Type", "application/json").body(body).build()

    @Test fun authAndPermissionAreFailures() = runBlocking {
        server().use { server ->
            server.enqueue(MockResponse.Builder().code(401).build())
            server.enqueue(MockResponse.Builder().code(401).build())
            val auth = assertIs<ConnectionResult.Failure>(SourceHttpClient(tokenProvider, clock).fetch(request(server), Page.serializer()))
            assertEquals(ConnectionFailure.AUTH_REQUIRED, auth.reason)
            assertEquals(SourceState.AUTH_REQUIRED, auth.status.state)
            assertEquals(1, tokenProvider.refreshes)
            assertEquals("Bearer fixture-secret-token", server.takeRequest().headers["Authorization"])
            assertEquals("Bearer fixture-refreshed-token", server.takeRequest().headers["Authorization"])
            server.enqueue(MockResponse.Builder().code(403).build())
            val denied = assertIs<ConnectionResult.Failure>(SourceHttpClient(tokenProvider, clock).fetch(request(server), Page.serializer()))
            assertEquals(SourceState.PERMISSION_DENIED, denied.status.state)
        }
    }

    @Test fun retryAfter60DoesNotLoop() = runBlocking {
        server().use { server ->
            server.enqueue(MockResponse.Builder().code(429).addHeader("Retry-After", "60").build())
            val result = assertIs<ConnectionResult.Failure>(SourceHttpClient(tokenProvider, clock).fetch(request(server), Page.serializer()))
            assertEquals(ConnectionFailure.RATE_LIMITED, result.reason)
            assertEquals(Instant.parse("2026-10-25T00:01:00Z"), result.retryAt)
            assertEquals(1, server.requestCount)
        }
    }

    @Test fun unknownRedirectNeverReceivesAuthorization() = runBlocking {
        server().use { origin -> server().use { other ->
            origin.enqueue(MockResponse.Builder().code(302).addHeader("Location", other.url("/steal")).build())
            val result = assertIs<ConnectionResult.Failure>(SourceHttpClient(tokenProvider, clock).fetch(request(origin), Page.serializer()))
            assertEquals(ConnectionFailure.REDIRECT_BLOCKED, result.reason)
            assertEquals(0, other.requestCount)
        } }
    }

    @Test fun paginationAndBadResponses() = runBlocking {
        server().use { server ->
            server.enqueue(json("""{"items":[1],"next":"/page2"}"""))
            server.enqueue(json("""{"items":[2]}"""))
            val result = assertIs<ConnectionResult.Success<List<Page>>>(SourceHttpClient(tokenProvider, clock).fetchPages(request(server), Page.serializer(), { it.next }))
            assertEquals(listOf(1, 2), result.value.flatMap { it.items })
            assertEquals(2, server.requestCount)
            server.enqueue(MockResponse.Builder().code(200).addHeader("Content-Type", "text/html").body("<html>error</html>").build())
            assertEquals(ConnectionFailure.INVALID_RESPONSE,
                assertIs<ConnectionResult.Failure>(SourceHttpClient(tokenProvider, clock).fetch(request(server), Page.serializer())).reason)
            server.enqueue(json("""{"items":[1,2,3]}"""))
            assertEquals(ConnectionFailure.RESPONSE_TOO_LARGE,
                assertIs<ConnectionResult.Failure>(SourceHttpClient(tokenProvider, clock, maxResponseBytes = 8).fetch(request(server), Page.serializer())).reason)
        }
    }

    @Test fun postRetryRequiresExplicitContract() = runBlocking {
        server().use { server ->
            server.enqueue(MockResponse.Builder().code(503).build())
            val post = request(server).copy(method = SourceRequest.Method.POST, jsonBody = "{}")
            assertEquals(ConnectionFailure.PROVIDER_ERROR,
                assertIs<ConnectionResult.Failure>(SourceHttpClient(tokenProvider, clock).fetch(post, Page.serializer())).reason)
            assertEquals(1, server.requestCount)
            server.enqueue(MockResponse.Builder().code(503).build())
            server.enqueue(json("""{"items":[]}"""))
            val safe = post.copy(postRetryContract = PostRetryContract.ProviderIdempotencyKey("fixture_key_123"))
            val retried = assertIs<ConnectionResult.Success<Page>>(SourceHttpClient(tokenProvider, clock).fetch(safe, Page.serializer()))
            assertEquals(emptyList(), retried.value.items)
            assertEquals(3, server.requestCount)
            server.takeRequest()
            assertEquals("fixture_key_123", server.takeRequest().headers["Idempotency-Key"])
            assertEquals("fixture_key_123", server.takeRequest().headers["Idempotency-Key"])
        }
    }

    @Test fun offlineAndTimeoutAreNotEmptySuccess() = runBlocking {
        server().use { server ->
            val endpoint = SourceEndpoint(server.url("/").toString())
            server.close()
            val offline = SourceHttpClient(tokenProvider, clock).fetch(SourceRequest(source, endpoint, "/items"), Page.serializer())
            assertEquals(ConnectionFailure.OFFLINE, assertIs<ConnectionResult.Failure>(offline).reason)
        }
        server().use { server ->
            server.enqueue(MockResponse.Builder().code(200).addHeader("Content-Type", "application/json")
                .body("""{"items":[]}""").headersDelay(1, TimeUnit.SECONDS).build())
            val shortClient = OkHttpClient.Builder().readTimeout(100, TimeUnit.MILLISECONDS).callTimeout(200, TimeUnit.MILLISECONDS).build()
            val timeout = SourceHttpClient(tokenProvider, clock, client = shortClient).fetch(request(server), Page.serializer())
            assertEquals(ConnectionFailure.TIMED_OUT, assertIs<ConnectionResult.Failure>(timeout).reason)
        }
    }

    @Test fun telemetryContainsOnlyStructuralCodes() = runBlocking {
        val captured = mutableListOf<String>()
        val sink = NetworkEventSink { kind, event -> captured += "${kind.name}:${event.name}" }
        server().use { server ->
            server.enqueue(MockResponse.Builder().code(403).body("fixture-email-body fixture-bank-account-123").build())
            SourceHttpClient(tokenProvider, clock, events = sink).fetch(request(server), Page.serializer())
            val output = captured.joinToString()
            assertEquals("MICROSOFT_MAIL:PERMISSION_DENIED", output)
            assertFalse(output.contains("fixture-secret-token"))
            assertFalse(output.contains("fixture-email-body"))
            assertFalse(output.contains("fixture-bank-account-123"))
        }
    }

    @Test fun coroutineCancellationCancelsInFlightCall() = runBlocking {
        server().use { server ->
            server.enqueue(MockResponse.Builder().code(200).addHeader("Content-Type", "application/json")
                .body("""{"items":[]}""").headersDelay(10, TimeUnit.SECONDS).build())
            val pending = async(Dispatchers.Default) { SourceHttpClient(tokenProvider, clock).fetch(request(server), Page.serializer()) }
            assertTrue(server.takeRequest(2, TimeUnit.SECONDS) != null)
            withTimeout(2_000) { pending.cancelAndJoin() }
            assertTrue(pending.isCancelled)
        }
    }
}
