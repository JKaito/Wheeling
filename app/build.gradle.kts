plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.android.libraries.mapsplatform.secrets.gradle.plugin)
}

android {
    namespace = "com.example.wheeling"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.wheeling"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation ("androidx.core:core-splashscreen:1.0.1")
    implementation(libs.constraintlayout)
    implementation("com.google.android.gms:play-services-maps:18.1.0")
    implementation(libs.play.services.location)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.google.maps.android:android-maps-utils:2.2.5")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation ("com.google.android.libraries.places:places:4.1.0")
    // core Glide runtime
    implementation ("com.github.bumptech.glide:glide:4.12.0")
    // annotation processor for generated API
    annotationProcessor ("com.github.bumptech.glide:compiler:4.12.0")
}