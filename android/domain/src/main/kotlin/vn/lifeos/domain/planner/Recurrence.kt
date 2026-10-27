package vn.lifeos.domain.planner

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import vn.lifeos.domain.common.Uuid

enum class Frequency { DAILY, WEEKLY, MONTHLY }

data class RecurrenceSpec(
    val frequency: Frequency,
    val interval: Int = 1,
    val untilInclusive: LocalDate? = null,
    val count: Int? = null,
) {
    init {
        require(interval in 1..30)
        require(count == null || count in 1..366)
        require(untilInclusive == null || count == null) { "Choose an end date or count" }
    }

    fun encode(): String = buildString {
        append(frequency.name)
        append(";INTERVAL=").append(interval)
        untilInclusive?.let { append(";UNTIL=").append(it) }
        count?.let { append(";COUNT=").append(it) }
    }

    companion object {
        fun parse(value: String): RecurrenceSpec? = try {
            val parts = value.split(';')
            if (parts.size !in 1..3) return null
            val frequency = Frequency.valueOf(parts.first())
            val options = parts.drop(1).associate {
                val pair = it.split('=', limit = 2)
                require(pair.size == 2 && pair[1].isNotBlank())
                pair[0] to pair[1]
            }
            if (options.size != parts.size - 1 || options.keys.any { it !in setOf("INTERVAL", "UNTIL", "COUNT") }) return null
            RecurrenceSpec(frequency, options["INTERVAL"]?.toInt() ?: 1,
                options["UNTIL"]?.let(LocalDate::parse), options["COUNT"]?.toInt())
        } catch (_: Exception) { null }
    }
}

/** Missing days (for example February 31) are skipped and do not consume COUNT. */
fun boundedOccurrences(
    seriesId: Uuid, firstDate: LocalDate, spec: RecurrenceSpec,
    startInclusive: LocalDate, endExclusive: LocalDate,
): List<LocalOccurrence> {
    val windowDays = ChronoUnit.DAYS.between(startInclusive, endExclusive)
    require(windowDays in 1..366)
    require(!endExclusive.isAfter(firstDate.plusYears(5).plusDays(1)))
    require(spec.untilInclusive == null || (!spec.untilInclusive.isBefore(firstDate) && !spec.untilInclusive.isAfter(firstDate.plusYears(5))))
    val results = mutableListOf<LocalOccurrence>()
    var emitted = 0
    for (ordinal in 0..1826) {
        val date = when (spec.frequency) {
            Frequency.DAILY -> firstDate.plusDays(ordinal.toLong() * spec.interval)
            Frequency.WEEKLY -> firstDate.plusWeeks(ordinal.toLong() * spec.interval)
            Frequency.MONTHLY -> {
                val month = YearMonth.from(firstDate).plusMonths(ordinal.toLong() * spec.interval)
                if (firstDate.dayOfMonth > month.lengthOfMonth()) null else month.atDay(firstDate.dayOfMonth)
            }
        }
        if (date == null) continue
        if (!date.isBefore(endExclusive) || date.isAfter(firstDate.plusYears(5)) ||
            (spec.untilInclusive != null && date.isAfter(spec.untilInclusive))) break
        emitted++
        if (spec.count != null && emitted > spec.count) break
        if (!date.isBefore(startInclusive)) results += LocalOccurrence(date, occurrenceKey(seriesId, date))
    }
    return results
}
