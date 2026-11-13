package vn.lifeos.notificationfixture;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Bundle;

/** Synthetic sender in a separate UID, exclusively for emulator integration tests. No banking APIs/data. */
public final class PublisherActivity extends Activity {
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (getIntent().getBooleanExtra("clear", false)) {
            manager.cancelAll();
        } else {
            manager.createNotificationChannel(new NotificationChannel("fixture", "Synthetic fixture", NotificationManager.IMPORTANCE_DEFAULT));
            Notification.Builder builder = new Notification.Builder(this, "fixture")
                .setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("Fixture Bank");
            String body = getIntent().getStringExtra("text");
            if (body != null) builder.setContentText(body);
            if (getIntent().getBooleanExtra("summary", false)) builder.setGroup("fixture").setGroupSummary(true);
            manager.notify("noti-fixture", getIntent().getIntExtra("id", 1), builder.build());
        }
        finish();
    }
}
