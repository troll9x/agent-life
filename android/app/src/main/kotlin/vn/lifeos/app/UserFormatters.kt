package vn.lifeos.app

import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

/** Takes Profile.zoneId and Profile.baseCurrency once onboarding supplies a profile. */
internal class UserFormatters(
    zoneId: String,
    currencyCode: String,
    private val locale: Locale = Locale.forLanguageTag("vi-VN"),
) {
    private val zone = ZoneId.of(zoneId)
    private val currency = Currency.getInstance(currencyCode)

    fun dateTime(instant: Instant): String = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", locale)
        .withZone(zone)
        .format(instant)

    fun money(amountMinor: Long): String {
        val formatter = NumberFormat.getCurrencyInstance(locale)
        formatter.currency = currency
        val divisor = java.math.BigDecimal.TEN.pow(currency.defaultFractionDigits)
        return formatter.format(java.math.BigDecimal.valueOf(amountMinor).divide(divisor))
    }
}
