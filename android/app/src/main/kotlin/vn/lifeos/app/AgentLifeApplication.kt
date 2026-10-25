package vn.lifeos.app

import android.app.Application
import vn.lifeos.data.security.EncryptedDatabaseProvider
import vn.lifeos.data.privacy.RetentionScheduler

class AgentLifeApplication : Application() {
    val databaseProvider: EncryptedDatabaseProvider by lazy { EncryptedDatabaseProvider(this) }

    override fun onCreate() {
        super.onCreate()
        RetentionScheduler.schedule(this)
    }
}
