package vn.lifeos.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import vn.lifeos.data.security.DatabaseAccess

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val provider = (application as AgentLifeApplication).databaseProvider
        setContent {
            val access by produceState<DatabaseAccess?>(initialValue = null, provider) {
                value = provider.open()
            }
            MaterialTheme {
                Surface {
                    DatabaseAccessScreen(access)
                }
            }
        }
    }
}

@Composable
internal fun DatabaseAccessScreen(access: DatabaseAccess?) {
    when (access) {
        null -> Text("Đang mở dữ liệu...")
        is DatabaseAccess.Ready -> Text("Agent Life")
        DatabaseAccess.UnlockDeviceFirst -> Text("Mở khóa thiết bị để tiếp tục.")
        DatabaseAccess.RecoveryRequired -> Text(
            "Không thể mở dữ liệu đã lưu. Ứng dụng không xóa dữ liệu hoặc tạo kho mới. " +
                "Hãy giữ nguyên ứng dụng và khôi phục từ bản sao lưu đã xuất. " +
                "Nếu chưa có bản sao lưu, hãy tìm trợ giúp trước khi tiếp tục.",
        )
        DatabaseAccess.StorageUnavailable -> Text("Không thể khởi tạo kho dữ liệu. Hãy giữ nguyên dữ liệu và thử lại sau khi cập nhật ứng dụng.")
    }
}
