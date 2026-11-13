package vn.lifeos.app.banknotifications

import java.security.MessageDigest
import vn.lifeos.data.notifications.FilteredNotification

/** The content supplier must not be invoked for an unselected package or a group summary. */
internal object NotificationFilter {
    private val otp = Regex("\\b(otp|one[ -]time (password|code)|verification code|authentication code|security code)\\b|mã (xác thực|bảo mật|đăng nhập)|ma (xac thuc|bao mat|dang nhap)", RegexOption.IGNORE_CASE)
    private val hidden = Regex("^(notification|new notification|content hidden|sensitive (notification )?content hidden|notification content hidden|nội dung (đã )?ẩn|thông báo mới)$", RegexOption.IGNORE_CASE)

    fun filter(packageName: String, allowed: Set<String>, summary: Boolean, key: String, postTime: Long,
        content: () -> Pair<String?, String?>): FilteredNotification? {
        if (packageName !in allowed || summary) return null
        val (rawTitle, rawBody) = content()
        val title = rawTitle?.take(256)?.trim()?.takeIf { it.isNotEmpty() && !hidden.matches(it) }
        val body = rawBody?.take(4096)?.trim()?.takeIf { it.isNotEmpty() && !hidden.matches(it) }
        // Conservative: do not retain any message explicitly marked as an authentication code.
        if (otp.containsMatchIn("${title.orEmpty()}\n${body.orEmpty()}")) return null
        val unavailable = body == null
        return FilteredNotification(packageName, hash(key), postTime,
            hash("${title.orEmpty().length}:${title.orEmpty()}\n${body.orEmpty()}"),
            title.takeUnless { unavailable }, body, unavailable)
    }

    private fun hash(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}
