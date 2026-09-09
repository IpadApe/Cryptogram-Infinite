plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// Release BuildConfig values come from gradle.properties (see CRYPTOGRAM_* keys).
// Debug builds use Google-provided AdMob test unit IDs.
fun prop(name: String, default: String): String =
    (project.findProperty(name) as String?)?.takeIf { it.isNotBlank() } ?: default

android {
    namespace = "dev.milan.cryptogram"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "dev.milan.cryptogram"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField("String", "ADMOB_APP_ID", "\"ca-app-pub-3940256099942544~3347511713\"")
            buildConfigField("String", "BANNER_UNIT_ID", "\"ca-app-pub-3940256099942544/6300978111\"")
            buildConfigField("String", "REWARDED_UNIT_ID", "\"ca-app-pub-3940256099942544/5224354917\"")
            buildConfigField(
                "String",
                "CONTENT_BASE_URL",
                "\"https://raw.githubusercontent.com/IpadApe/Cryptogram-Infinite/main/\""
            )
            manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val admobAppId = prop("CRYPTOGRAM_ADMOB_APP_ID", "ca-app-pub-3940256099942544~3347511713")
            buildConfigField("String", "ADMOB_APP_ID", "\"$admobAppId\"")
            buildConfigField(
                "String",
                "BANNER_UNIT_ID",
                "\"${prop("CRYPTOGRAM_BANNER_UNIT_ID", "ca-app-pub-3940256099942544/6300978111")}\""
            )
            buildConfigField(
                "String",
                "REWARDED_UNIT_ID",
                "\"${prop("CRYPTOGRAM_REWARDED_UNIT_ID", "ca-app-pub-3940256099942544/5224354917")}\""
            )
            buildConfigField(
                "String",
                "CONTENT_BASE_URL",
                "\"${prop("CRYPTOGRAM_CONTENT_BASE_URL", "https://raw.githubusercontent.com/IpadApe/Cryptogram-Infinite/main/")}\""
            )
            manifestPlaceholders["admobAppId"] = admobAppId
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
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        optIn.add("androidx.compose.material3.ExperimentalMaterial3Api")
        optIn.add("androidx.compose.foundation.ExperimentalFoundationApi")
        optIn.add("androidx.compose.foundation.layout.ExperimentalLayoutApi")
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)
    implementation(libs.billing.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
