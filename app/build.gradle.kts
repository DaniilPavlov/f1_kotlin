import java.io.FileInputStream
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        load(FileInputStream(file))
    }
}

fun prop(name: String, default: String = ""): String =
    localProperties.getProperty(name, System.getProperty(name, default))

// google-services.json is gitignored. Copy CI stub at configuration time so bare
// `./gradlew assemble` (CodeQL default autobuild) works without a pre-step.
val googleServicesFile = file("google-services.json")
val googleServicesStub = rootProject.file("tool/ci/google-services.stub.json")
if (!googleServicesFile.exists()) {
    googleServicesStub.copyTo(googleServicesFile)
    logger.lifecycle("Copied tool/ci/google-services.stub.json → app/google-services.json")
}
val isFirebaseCiStub =
    googleServicesFile.readText().contains("\"project_id\": \"ci-stub\"")
if (isFirebaseCiStub) {
    logger.lifecycle("Firebase google-services.json is CI stub — Crashlytics mapping upload disabled")
}
android {
    namespace = "com.example.f1_kotlin"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.f1_kotlin"
        minSdk = 30
        targetSdk = 37
        versionCode = 202608040
        versionName = "2.0.0"
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
            isMinifyEnabled = true
            isShrinkResources = true
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

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

// Business-logic coverage gate.
kover {
    reports {
        filters {
            excludes {
                classes(
                    "*.BuildConfig",
                    "*.R",
                    "*.R$*",
                    "*ComposableSingletons*",
                    "*_Factory*",
                    "*_HiltModules*",
                    "*_MembersInjector*",
                    "*_Impl*",
                    "com.example.f1_kotlin.F1Application*",
                    "com.example.f1_kotlin.MainActivity*",
                    "com.example.f1_kotlin.Hilt_MainActivity*",
                    "com.example.f1_kotlin.AppKt*",
                    "com.example.f1_kotlin.util.ShareHelper*",
                    "com.example.f1_kotlin.util.AppLogger*",
                    "com.example.f1_kotlin.util.TrustedUrlKt*",
                    "com.example.f1_kotlin.data.analytics.AppAnalyticsGateway*",
                    "com.example.f1_kotlin.data.circuits.CircuitLayoutAssets*",
                    "com.example.f1_kotlin.data.circuits.CircuitStats*",
                    "com.example.f1_kotlin.data.circuits.CircuitStatsRepository*",
                    "com.example.f1_kotlin.data.repository.IF1Repository*",
                    "com.example.f1_kotlin.data.repository.IEspnRepository*",
                    "com.example.f1_kotlin.domain.ThemePreferences*",
                    "com.example.f1_kotlin.domain.LocalePreferences*",
                    "com.example.f1_kotlin.ui.views.*",
                    "com.example.f1_kotlin.ui.map.*",
                )
                annotatedBy(
                    "androidx.compose.runtime.Composable",
                    "androidx.compose.ui.tooling.preview.Preview",
                    "dagger.Module",
                    "dagger.internal.DaggerGenerated",
                )
                packages(
                    "dagger.hilt.internal.aggregatedroot.codegen",
                    "dagger.hilt.internal.aggregatedroot.codegen.*",
                    "hilt_aggregated_deps",
                    "hilt_aggregated_deps.*",
                    "com.example.f1_kotlin.ui",
                    "com.example.f1_kotlin.ui.*",
                    "com.example.f1_kotlin.widgets",
                    "com.example.f1_kotlin.widgets.*",
                    "com.example.f1_kotlin.di",
                    "com.example.f1_kotlin.di.*",
                    "com.example.f1_kotlin.notifications",
                    "com.example.f1_kotlin.notifications.*",
                    "com.example.f1_kotlin.data.firebase",
                    "com.example.f1_kotlin.data.firebase.*",
                    "com.example.f1_kotlin.data.appmetrica",
                    "com.example.f1_kotlin.data.appmetrica.*",
                    "com.example.f1_kotlin.data.model",
                    "com.example.f1_kotlin.data.model.*",
                    "com.example.f1_kotlin.data.api",
                    "com.example.f1_kotlin.data.api.*",
                    "com.example.f1_kotlin.data.local",
                    "com.example.f1_kotlin.data.local.*",
                )
            }
        }
        verify {
            rule {
                minBound(75)
            }
        }
    }
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
    implementation(libs.androidx.lifecycle.runtime.compose)
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
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.appmetrica.analytics)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
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

// Stub google-services.json has no real Firebase project — mapping upload would 400.
afterEvaluate {
    if (isFirebaseCiStub) {
        tasks.matching { it.name.contains("uploadCrashlyticsMappingFile", ignoreCase = true) }
            .configureEach { enabled = false }
    }
}
