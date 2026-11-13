package vn.lifeos.app.banknotifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import vn.lifeos.app.AgentLifeApplication

class BankNotificationListenerService : NotificationListenerService() {
    private val runtime get() = (application as AgentLifeApplication).appContainer.bankNotifications
    override fun onListenerConnected() { super.onListenerConnected(); runtime.connectionChanged(true) }
    override fun onListenerDisconnected() { runtime.connectionChanged(false); super.onListenerDisconnected() }
    override fun onNotificationPosted(sbn: StatusBarNotification) { runtime.receive(sbn) }
    override fun onDestroy() { runtime.connectionChanged(false); super.onDestroy() }
}
