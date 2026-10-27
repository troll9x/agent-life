package vn.lifeos.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import vn.lifeos.data.profile.ProfileInput
import vn.lifeos.data.security.DatabaseAccess
import vn.lifeos.domain.foundation.Outcome

@RunWith(AndroidJUnit4::class)
class DeviceZoneSyncTest {
    @Test fun deviceZoneUpdatePersistsWithoutChangingCurrency() = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as AgentLifeApplication
        val container = app.appContainer
        val access = container.startupRepository.open() as DatabaseAccess.Ready
        val store = container.profileStore(access.database)
        val existing = (store.observe().first() as Outcome.Success).value
        if (existing == null) {
            assertTrue(store.complete(ProfileInput("Fixture Zone", ZoneId.systemDefault().id, "VND", 1)) is Outcome.Success)
        }
        val model = ShellViewModel.factory(container).create(ShellViewModel::class.java)
        val original = withTimeout(20_000) { (model.state.first { it.profile is ProfileStatus.Ready }.profile as ProfileStatus.Ready).snapshot.profile }
        val changedZone = if (original.zoneId == "Pacific/Auckland") "Asia/Ho_Chi_Minh" else "Pacific/Auckland"
        try {
            model.syncDeviceTimeZone(changedZone)
            val changed = withTimeout(20_000) {
                (model.state.first { (it.profile as? ProfileStatus.Ready)?.snapshot?.profile?.zoneId == changedZone }
                    .profile as ProfileStatus.Ready).snapshot.profile
            }
            assertEquals(original.baseCurrency, changed.baseCurrency)
        } finally {
            model.syncDeviceTimeZone(original.zoneId)
            withTimeout(20_000) {
                model.state.first { (it.profile as? ProfileStatus.Ready)?.snapshot?.profile?.zoneId == original.zoneId }
            }
        }
    }
}
