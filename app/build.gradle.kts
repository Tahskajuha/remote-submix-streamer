val appName = "remoteSubmixStreamer"
val packageName = "com.example.remotesubmixstreamer"

plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  id("org.jetbrains.kotlin.plugin.compose")
}

android {
  namespace = packageName
  compileSdk = 36

  defaultConfig {
    applicationId = packageName
    minSdk = 26
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"
  }

  buildTypes {
    release {
      isMinifyEnabled = false
    }
  }

  buildFeatures {
    compose = true
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }

  kotlin {
    jvmToolchain(21)
  }
}

dependencies {
  implementation(platform("androidx.compose:compose-bom:2026.03.00"))
  implementation("androidx.core:core-ktx")
  implementation("androidx.lifecycle:lifecycle-runtime-ktx")
  debugImplementation("androidx.compose.ui:ui-tooling")
  implementation("androidx.compose.ui:ui-tooling-preview")
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.material3:material3")
  implementation("androidx.activity:activity-compose")
  implementation("androidx.appcompat:appcompat:1.7.1")
  implementation("eu.buney.kopus:kopus-full:1.6.1.2")
}
