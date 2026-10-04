import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun localProp(key: String): String = localProps.getProperty(key, "")

// Keystore signing
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.saivatsal.soundorbit"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.saivatsal.soundorbit"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "1.2.0"

        testInstrumentationRunner = "com.saivatsal.soundorbit.HiltTestRunner"

        buildConfigField("String", "AUDIUS_API_KEY", "\"${localProp("AUDIUS_API_KEY")}\"")
        buildConfigField("String", "AUDIUS_BEARER_TOKEN", "\"${localProp("AUDIUS_BEARER_TOKEN")}\"")
        buildConfigField("String", "JAMENDO_CLIENT_ID", "\"${localProp("JAMENDO_CLIENT_ID")}\"")
        buildConfigField("String", "SPOTIFY_CLIENT_ID", "\"${localProp("SPOTIFY_CLIENT_ID")}\"")
        buildConfigField("String", "RAPIDAPI_DEEZER_KEY", "\"${localProp("RAPIDAPI_DEEZER_KEY")}\"")
        buildConfigField("String", "RAPIDAPI_DEEZER_KEYS", "\"${localProp("RAPIDAPI_DEEZER_KEYS").ifEmpty { localProp("RAPIDAPI_DEEZER_KEY") }}\"")
        buildConfigField("String", "CONTACT_EMAIL", "\"${localProp("CONTACT_EMAIL")}\"")
    }

    signingConfigs {
        if (keystoreProps.containsKey("storeFile")) {
            create("release") {
                storeFile = file(keystoreProps["storeFile"] as String)
                storePassword = keystoreProps["storePassword"] as String
                keyAlias = keystoreProps["keyAlias"] as String
                keyPassword = keystoreProps["keyPassword"] as String
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = false
            }
        }
    }

    buildTypes {
        debug {
            // No applicationIdSuffix so debug and release update over each other
            if (keystoreProps.containsKey("storeFile")) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystoreProps.containsKey("storeFile")) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
            all {
                it.jvmArgs(
                    "-Djdk.lang.Process.allowAmbiguousCommands=true",
                    "-Djava.io.tmpdir=C:/temp",
                    "-Dfile.encoding=UTF-8"
                )
            }
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    room {
        schemaDirectory("$projectDir/schemas")
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.compose.material3)
    implementation(libs.compose.material3.window)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.animation)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    // AndroidX
    implementation(libs.activity.compose)
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.splashscreen)
    implementation(libs.palette)

    // Navigation
    implementation(libs.navigation.compose)

    // Lifecycle
    implementation(libs.lifecycle.runtime)
    implementation(libs.lifecycle.viewmodel)

    // Media3
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.media3.ui)
    implementation(libs.media3.datasource)
    implementation(libs.media3.datasource.okhttp)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Moshi
    implementation(libs.moshi)
    ksp(libs.moshi.codegen)

    // Image
    implementation(libs.coil.compose)
    implementation(libs.coil.network)

    // DataStore
    implementation(libs.datastore)

    // WorkManager
    implementation(libs.workmanager)

    // Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.robolectric)
    testImplementation(libs.room.testing)
    testImplementation(libs.hilt.testing)
    kspTest(libs.hilt.compiler)

    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.espresso)
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.hilt.testing)
    kspAndroidTest(libs.hilt.compiler)
}
