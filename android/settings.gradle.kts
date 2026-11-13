pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AgentLife"
include(":app", ":domain", ":data")
// Standalone synthetic notification sender; never a dependency of the production APK.
include(":notification-fixture")
project(":notification-fixture").projectDir = file("test-fixtures/notification-publisher")
