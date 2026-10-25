package vn.lifeos.domain.integration

import vn.lifeos.domain.common.AppError
import vn.lifeos.domain.common.AppErrorCode
import vn.lifeos.domain.common.SourceRef
import vn.lifeos.domain.common.SourceState
import vn.lifeos.domain.common.SourceStatus
import java.time.Instant

/** Credentials belong to the connector, never to HTTP logging or the request DTO. */
interface AccessTokenProvider {
    suspend fun tokenFor(source: SourceRef): String?
    /** Return a freshly issued token, or null when reauthentication is required. */
    suspend fun refresh(source: SourceRef): String?
}

enum class ConnectionFailure {
    DISCONNECTED, AUTH_REQUIRED, PERMISSION_DENIED, RATE_LIMITED, OFFLINE, TIMED_OUT,
    PROVIDER_ERROR, INVALID_RESPONSE, RESPONSE_TOO_LARGE, REDIRECT_BLOCKED,
}

sealed interface ConnectionResult<out T> {
    data class Success<T>(val value: T, val status: SourceStatus) : ConnectionResult<T>
    data class Failure(
        val reason: ConnectionFailure,
        val status: SourceStatus,
        val retryAt: Instant? = null,
    ) : ConnectionResult<Nothing>
}

fun connectionFailure(reason: ConnectionFailure, checkedAt: Instant, retryAt: Instant? = null): ConnectionResult.Failure {
    val state = when (reason) {
        ConnectionFailure.DISCONNECTED -> SourceState.NOT_CONNECTED
        ConnectionFailure.AUTH_REQUIRED -> SourceState.AUTH_REQUIRED
        ConnectionFailure.PERMISSION_DENIED -> SourceState.PERMISSION_DENIED
        else -> SourceState.ERROR
    }
    val code = when (reason) {
        ConnectionFailure.DISCONNECTED -> AppErrorCode.NOT_FOUND
        ConnectionFailure.AUTH_REQUIRED -> AppErrorCode.AUTH_REQUIRED
        ConnectionFailure.PERMISSION_DENIED -> AppErrorCode.PERMISSION_DENIED
        ConnectionFailure.OFFLINE, ConnectionFailure.TIMED_OUT, ConnectionFailure.RATE_LIMITED -> AppErrorCode.NETWORK
        else -> AppErrorCode.PROVIDER_ERROR
    }
    return ConnectionResult.Failure(reason, SourceStatus(state, checkedAt = checkedAt, error = AppError(code)), retryAt)
}
