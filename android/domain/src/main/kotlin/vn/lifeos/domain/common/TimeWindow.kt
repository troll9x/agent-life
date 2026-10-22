package vn.lifeos.domain.common

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/** A half-open interval of instants. */
data class TimeWindow(val startInclusive: Instant, val endExclusive: Instant) {
    init {
        require(startInclusive < endExclusive) { "Time window must be nonempty" }
    }

    operator fun contains(instant: Instant): Boolean =
        instant >= startInclusive && instant < endExclusive
}

/** Local calendar dates; the original zone remains explicit for conversion. */
data class LocalDateRange(
    val startInclusive: LocalDate,
    val endExclusive: LocalDate,
    val zoneId: ZoneId,
) {
    init {
        require(startInclusive < endExclusive) { "Date range must be nonempty" }
    }

    fun toTimeWindow(): TimeWindow = TimeWindow(
        startInclusive.atStartOfDay(zoneId).toInstant(),
        endExclusive.atStartOfDay(zoneId).toInstant(),
    )
}

fun localNow(clock: Clock, zoneId: ZoneId): ZonedDateTime = clock.instant().atZone(zoneId)
