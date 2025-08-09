// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.google.android.libraries.mapsplatform.secrets.gradle.plugin) apply false

    // Firebase / Google services Gradle plugin (top-level, not applied here)
    id("com.google.gms.google-services") version "4.4.2" apply false
}
