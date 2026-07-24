import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.detekt)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        load(FileInputStream(file))
    }
}

fun prop(name: String, default: String = ""): String =
    localProperties.getProperty(name, System.getProperty(name, default))

android {
    namespace = "com.example.f1_kotlin"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.f1_kotlin"
        minSdk = 30
        targetSdk = 36
        versionCode = 202607240
        versionName = "1.5.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Empty until set in local.properties / CI — bootstrap skips AppMetrica.
        buildConfigField("String", "APPMETRICA_API_KEY", "\"${prop("appmetrica.apiKey")}\"")
    }

    val keystorePropertiesFile = rootProject.file("key.properties")
    val keystoreProperties = Properties()
    if (keystorePropertiesFile.exists()) {
        keystoreProperties.load(FileInputStream(keystorePropertiesFile))
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (keystorePropertiesFile.exists()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// google-services.json is gitignored; use CI stub when missing.
val googleServicesFile = file("google-services.json")
val googleServicesStub = rootProject.file("tool/ci/google-services.stub.json")
tasks.register("ensureGoogleServicesJson") {
    doLast {
        if (!googleServicesFile.exists()) {
            googleServicesStub.copyTo(googleServicesFile, overwrite = true)
            logger.lifecycle("Copied tool/ci/google-services.stub.json → app/google-services.json")
        }
    }
}
tasks.matching { it.name.startsWith("process") && it.name.endsWith("GoogleServices") }.configureEach {
    dependsOn("ensureGoogleServicesJson")
}
tasks.named("preBuild").configure { dependsOn("ensureGoogleServicesJson") }

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("$rootDir/config/detekt/detekt.yml")
}

configurations.configureEach {
    resolutionStrategy {
        // Avoid androidx.fragment:1.5.4 which Google Maven intermittently 404s.
        force("androidx.fragment:fragment:1.8.9")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)
    implementation(libs.osmdroid)
    implementation(libs.osmbonuspack)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.coil.compose)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.config)
    implementation(libs.appmetrica.analytics)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// Must be applied after the Android Application plugin (AGP Variant API).
apply(plugin = "com.google.gms.google-services")
apply(plugin = "com.google.firebase.crashlytics")

// CI stub google-services.json — Crashlytics mapping upload would 400 against it.
if (googleServicesFile.exists() && googleServicesFile.readText().contains("\"project_id\": \"ci-stub\"")) {
    logger.lifecycle("Firebase google-services.json is CI stub — Crashlytics mapping upload disabled")
}
