package vn.lifeos.app

import android.app.Application
import vn.lifeos.data.security.EncryptedDatabaseProvider
import vn.lifeos.data.privacy.RetentionScheduler
import vn.lifeos.app.reminders.ReminderWorkSchedule
import vn.lifeos.app.reminders.createReminderChannel

class AgentLifeApplication : Application() {
    val databaseProvider: EncryptedDatabaseProvider by lazy { EncryptedDatabaseProvider(this) }
    val appContainer: AppContainer by lazy { AppContainer(this, databaseProvider) }

    override fun onCreate() {
        super.onCreate()
        RetentionScheduler.schedule(this)
        createReminderChannel(this)
        ReminderWorkSchedule.register(this)
    }
}
