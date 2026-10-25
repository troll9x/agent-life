package vn.lifeos.data.privacy

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import java.time.Clock
import java.util.concurrent.TimeUnit
import vn.lifeos.data.security.DatabaseAccess
import vn.lifeos.data.security.EncryptedDatabaseProvider

/** WorkManager is best effort; every read also excludes expired raw synchronously. */
class RetentionWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val provider = EncryptedDatabaseProvider(applicationContext)
        return when (val access = provider.open()) {
            is DatabaseAccess.Ready -> try {
                access.database.privacyDao().purgeExpired(Clock.systemUTC().millis())
                Result.success()
            } finally { provider.close() }
            DatabaseAccess.UnlockDeviceFirst, DatabaseAccess.StorageUnavailable -> Result.retry()
            DatabaseAccess.RecoveryRequired -> Result.failure()
        }
    }
}

object RetentionScheduler {
    fun schedule(context: Context) {
        val manager = WorkManager.getInstance(context)
        manager.enqueueUniquePeriodicWork("privacy-raw-retention", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<RetentionWorker>(1, TimeUnit.HOURS).build())
        manager.enqueueUniqueWork("privacy-raw-retention-startup", ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<RetentionWorker>().build())
    }
}
