import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.util.Properties

// Release signing comes from keystore.properties in the project root (never committed).
// See docs/RELEASE.md for how to create the keystore and this file.
val keystoreProps = Properties().apply {
  val f = rootProject.file("keystore.properties")
  if (f.exists()) f.inputStream().use { load(it) }
}
val hasReleaseKeystore = keystoreProps.getProperty("storeFile") != null
// Local-only escape hatch to test a minified build on a phone that has the debug build installed:
//   gradlew assembleRelease -PallowDebugSigning=true   (NEVER distribute that APK)
val allowDebugSigning = (findProperty("allowDebugSigning") as String?)?.toBoolean() == true

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.addin.mqzpvt"
    minSdk = 24
    targetSdk = 36
    // Bump BOTH for every release you distribute: versionCode must always increase.
    versionCode = 1
    versionName = "1.0.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      if (hasReleaseKeystore) {
        storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
        storePassword = keystoreProps.getProperty("storePassword")
        keyAlias = keystoreProps.getProperty("keyAlias")
        keyPassword = keystoreProps.getProperty("keyPassword")
      }
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      // R8: removes unused code/resources and obfuscates -> smaller, harder-to-copy APK.
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = when {
        hasReleaseKeystore -> signingConfigs.getByName("release")
        allowDebugSigning -> signingConfigs.getByName("debugConfig")
        else -> null // assembleRelease will stop with a clear "not signed" result; see docs/RELEASE.md
      }
    }
    debug { signingConfig = signingConfigs.getByName("debugConfig") }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  // implementation(libs.coil.compose)
  // implementation(libs.converter.moshi)  // unused - removed from release (smaller APK; App Check debug provider must never ship)
  // implementation(libs.firebase.ai)  // unused - removed from release (smaller APK; App Check debug provider must never ship)
  // Firestore
  implementation(libs.firebase.firestore)

  // Firebase Auth and Google Sign-In via Credential Manager:
  implementation(libs.firebase.auth)
  // implementation(libs.androidx.credentials)  // unused - removed from release (smaller APK; App Check debug provider must never ship)
  // implementation(libs.androidx.credentials.play.services)  // unused - removed from release (smaller APK; App Check debug provider must never ship)
  // implementation(libs.googleid)  // unused - removed from release (smaller APK; App Check debug provider must never ship)
  // implementation(libs.firebase.appcheck.recaptcha)  // unused - removed from release (smaller APK; App Check debug provider must never ship)
  // implementation(libs.firebase.appcheck.debug)  // unused - removed from release (smaller APK; App Check debug provider must never ship)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  // implementation(libs.logging.interceptor)  // unused - removed from release (smaller APK; App Check debug provider must never ship)
  // implementation(libs.moshi.kotlin)  // unused - removed from release (smaller APK; App Check debug provider must never ship)
  // implementation(libs.okhttp)  // unused - removed from release (smaller APK; App Check debug provider must never ship)
  implementation(libs.play.services.location)
  // implementation(libs.retrofit)  // unused - removed from release (smaller APK; App Check debug provider must never ship)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  // "ksp"(libs.moshi.kotlin.codegen)  // unused - removed from release (smaller APK; App Check debug provider must never ship)
}
