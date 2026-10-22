package vn.lifeos.domain.common

import java.time.Instant

enum class SourceKind {
    MANUAL,
    BANK_NOTIFICATION,
    MICROSOFT_MAIL,
    MICROSOFT_CALENDAR,
    GOOGLE_MAIL,
    GOOGLE_CALENDAR,
    HEALTH_CONNECT,
    NEWS,
    IMPORT,
}

data class SourceRef(
    val kind: SourceKind,
    val sourceId: String,
    val connectionId: Uuid? = null,
) {
    init {
        require(sourceId.isNotBlank()) { "Source ID must not be blank" }
    }
}

enum class SourceState {
    NOT_CONNECTED,
    OK,
    PARTIAL,
    STALE,
    AUTH_REQUIRED,
    PERMISSION_DENIED,
    UNSUPPORTED,
    ERROR,
}

/** Stable code only: provider error text and tokens must not enter domain state. */
enum class AppErrorCode {
    VALIDATION,
    NOT_FOUND,
    CONFLICT,
    AUTH_REQUIRED,
    PERMISSION_DENIED,
    UNSUPPORTED,
    NETWORK,
    PROVIDER_ERROR,
    UNKNOWN,
}

data class AppError(val code: AppErrorCode)

data class SourceStatus(
    val state: SourceState,
    val checkedAt: Instant? = null,
    val lastSuccessfulAt: Instant? = null,
    val error: AppError? = null,
) {
    init {
        require(state != SourceState.OK || error == null) { "Successful source has no error" }
    }
}
