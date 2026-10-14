import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// Release signing key lives outside the repo; without it the release build stays unsigned.
// CI passes it through environment variables, a local build reads ~/.xarajatlar-signing.
val keystoreProperties = Properties().apply {
    val file = File(System.getProperty("user.home"), ".xarajatlar-signing/keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
    mapOf(
        "storeFile" to "KEYSTORE_FILE",
        "storePassword" to "KEYSTORE_PASSWORD",
        "keyAlias" to "KEY_ALIAS",
        "keyPassword" to "KEY_PASSWORD"
    ).forEach { (key, env) -> System.getenv(env)?.let { setProperty(key, it) } }
}

android {
    namespace = "uz.jtscorp.xarajatlar"
    compileSdk = 35

    defaultConfig {
        applicationId = "uz.jtscorp.xarajatlar"
        minSdk = 35
        targetSdk = 35
        versionCode = 3
        versionName = "2.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (keystoreProperties.containsKey("storeFile")) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        viewBinding = true
    }
    lint {
        disable += "UnsafeOptInUsageError"
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)

    // Room
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

    // CameraX
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation(libs.camera.video)

    // ML Kit
    implementation(libs.mlkit.barcode)
    implementation(libs.mlkit.text)

    // Charts
    implementation(libs.mpandroidchart)

    // Security
    implementation(libs.biometric)
    implementation(libs.security.crypto)

    // Web scraping
    implementation(libs.jsoup)

    // Location
    implementation(libs.play.services.location)

    // Navigation
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    // Lifecycle
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.lifecycle.service)

    // WorkManager
    implementation(libs.work.runtime)

    // DocumentFile
    implementation(libs.documentfile)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
