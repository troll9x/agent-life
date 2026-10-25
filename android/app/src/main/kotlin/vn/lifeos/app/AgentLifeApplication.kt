package vn.lifeos.app

import android.app.Application
import vn.lifeos.data.security.EncryptedDatabaseProvider

class AgentLifeApplication : Application() {
    val databaseProvider: EncryptedDatabaseProvider by lazy { EncryptedDatabaseProvider(this) }
}
