plugins { alias(libs.plugins.android.application) }

android {
    namespace = "vn.lifeos.notificationfixture"
    compileSdk = 37
    defaultConfig {
        applicationId = "vn.lifeos.notificationfixture"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "fixture-only"
    }
}
