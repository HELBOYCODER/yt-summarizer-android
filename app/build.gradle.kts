import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
}

// Optional default Gemini key baked into the build (can also be set inside the app).
// Order: env GEMINI_API_KEY -> local.properties GEMINI_API_KEY -> empty.
val localProps = Properties().apply {
  val f = rootProject.file("local.properties")
  if (f.exists()) f.inputStream().use { load(it) }
}
val defaultGeminiKey: String =
  System.getenv("GEMINI_API_KEY") ?: localProps.getProperty("GEMINI_API_KEY") ?: ""

android {
  namespace = "com.erfan.ytsummarizer"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.erfan.ytsummarizer"
    minSdk = 24
    targetSdk = 35
    versionCode = 1
    versionName = "1.0"
    buildConfigField("String", "DEFAULT_GEMINI_KEY", "\"$defaultGeminiKey\"")
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      // Signed with the debug key so the APK installs directly without extra setup.
      signingConfig = signingConfigs.getByName("debug")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  kotlinOptions { jvmTarget = "17" }
  buildFeatures {
    compose = true
    buildConfig = true
  }
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.okhttp)
  implementation(libs.kotlinx.coroutines.android)
  debugImplementation(libs.androidx.compose.ui.tooling)
  testImplementation(libs.junit)
  testImplementation(libs.json)
}
