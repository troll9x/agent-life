package vn.lifeos.app.banknotifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.Clock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.Assume.assumeTrue
import org.junit.runner.RunWith
import vn.lifeos.app.AgentLifeApplication
import vn.lifeos.data.db.RoomFoundationRepository
import vn.lifeos.data.notifications.RoomNotificationRepository
import vn.lifeos.data.security.DatabaseAccess
import vn.lifeos.domain.common.RandomIdGenerator
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.foundation.CreatePersonalWorkspace
import vn.lifeos.domain.foundation.Outcome

/** Opt-in only: grants/revokes listener access solely on the named isolated test AVD. */
@RunWith(AndroidJUnit4::class)
class BankNotificationServiceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private fun shell(command: String): String = ParcelFileDescriptor.AutoCloseInputStream(
        instrumentation.uiAutomation.executeShellCommand(command)).bufferedReader().use { it.readText() }
    private suspend fun awaitCondition(predicate: suspend () -> Boolean) {
        repeat(100) { if (predicate()) return; delay(100) }
        assertTrue("Timed out waiting for listener/queue", predicate())
    }

    @Test fun systemListenerQueuesCrossPackageWhileUiClosedAndDetectsRevocation() = runBlocking {
        // Never change OS permissions on a user's phone or ordinary emulator by accident.
        assumeTrue(InstrumentationRegistry.getArguments().getString("isolatedNotificationTest") == "true")
        assertEquals("AgentLife_Isolated_Test_API_37", shell("getprop ro.boot.qemu.avd_name").trim())
        val app = instrumentation.targetContext.applicationContext as AgentLifeApplication
        val db = (app.databaseProvider.open() as DatabaseAccess.Ready).database
        if (db.foundationDao().activeWorkspace() == null) {
            assertTrue(RoomFoundationRepository(db, Clock.systemUTC(), RandomIdGenerator).createPersonalWorkspace(
                CreatePersonalWorkspace(RandomIdGenerator.newId(), RandomIdGenerator.newId(), RandomIdGenerator.newId(),
                    RandomIdGenerator.newId(), "Personal", "Notification fixture", "Asia/Ho_Chi_Minh", "VND", 1, "ANDROID", "test")) is Outcome.Success)
        }
        val workspace = db.foundationDao().activeWorkspace()!!
        val runtime = app.appContainer.bankNotifications
        val manager = app.getSystemService(NotificationManager::class.java)
        val component = ComponentName(app, BankNotificationListenerService::class.java)
        val originalGrant = manager.isNotificationListenerAccessGranted(component)
        val originalApps = db.notificationDao().apps(workspace.id, workspace.originDeviceId).map { it.packageName }.toSet()
        val publisher = "vn.lifeos.notificationfixture"
        assertTrue(shell("pm path $publisher").contains("package:"))
        // The harness grants/restores sender permissions outside instrumentation.
        assertEquals(PackageManager.PERMISSION_GRANTED, app.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS))
        val channel = "noti-fixture"
        val componentName = component.flattenToString()
        val repo = RoomNotificationRepository(db, Clock.systemUTC(), RandomIdGenerator) { manager.isNotificationListenerAccessGranted(component) }
        val before = db.notificationDao().observeCaptures(workspace.id, workspace.originDeviceId).first().size
        try {
            shell("cmd notification allow_listener $componentName")
            assertTrue(repo.selectPackages(Uuid.parse(workspace.id), setOf(publisher)) is Outcome.Success)
            runtime.refreshAccess()
            awaitCondition { runtime.state.value.connected && runtime.state.value.selected == setOf(publisher) }
            // No Activity is launched in this test. Send Android HOME to ensure UI is not foreground.
            shell("input keyevent KEYCODE_HOME")
            manager.createNotificationChannel(NotificationChannel(channel, "Fixture", NotificationManager.IMPORTANCE_DEFAULT))
            fun notification(context: android.content.Context, body: String?) = Notification.Builder(context, channel)
                .setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("Fixture Bank")
                .apply { if (body != null) setContentText(body) }.build()
            // Looks like banking content but originates from an unselected package.
            manager.notify("noti-fixture", 9100, notification(app, "SPOOF_FIXTURE credited 50000 VND"))
            shell("am start -W -n $publisher/.PublisherActivity --ei id 9101 --es text 'ALLOWED_FIXTURE credited 50000 VND'")
            shell("am start -W -n $publisher/.PublisherActivity --ei id 9102")
            shell("am start -W -n $publisher/.PublisherActivity --ei id 9104 --ez summary true --es text SUMMARY_FIXTURE")
            awaitCondition { db.notificationDao().observeCaptures(workspace.id, workspace.originDeviceId).first().size >= before + 2 }
            delay(500)
            assertEquals(before + 2, db.notificationDao().observeCaptures(workspace.id, workspace.originDeviceId).first().size)
            val rows = db.notificationDao().observeCaptures(workspace.id, workspace.originDeviceId).first()
                .filter { it.packageName == publisher }
            assertTrue(rows.any { it.body?.contains("ALLOWED_FIXTURE") == true })
            assertTrue(rows.any { it.contentUnavailable && it.body == null })
            assertFalse(db.notificationDao().observeCaptures(workspace.id, workspace.originDeviceId).first().any { it.body?.contains("SPOOF_FIXTURE") == true })
            assertTrue(db.financeInboxDao().observeOpen(workspace.id).first().any { it.amountMinor == null && it.missingFields.contains("amountMinor") })
            shell("cmd notification disallow_listener $componentName")
            awaitCondition { !runtime.state.value.permission && !runtime.state.value.connected }
            awaitCondition { db.notificationDao().connection(workspace.id, workspace.originDeviceId)?.state == "PERMISSION_DENIED" }
            assertTrue(db.notificationDao().observeCaptures(workspace.id, workspace.originDeviceId).first().all { it.body == null && it.title == null })
            val count = db.notificationDao().observeCaptures(workspace.id, workspace.originDeviceId).first().size
            shell("am start -W -n $publisher/.PublisherActivity --ei id 9103 --es text AFTER_REVOKE_FIXTURE")
            delay(500)
            assertEquals(count, db.notificationDao().observeCaptures(workspace.id, workspace.originDeviceId).first().size)
        } finally {
            shell("am start -W -n $publisher/.PublisherActivity --ez clear true")
            manager.cancel("noti-fixture", 9100)
            shell("cmd notification ${if (originalGrant) "allow_listener" else "disallow_listener"} $componentName")
            repo.selectPackages(Uuid.parse(workspace.id), originalApps)
            runtime.refreshAccess()
        }
    }
}
