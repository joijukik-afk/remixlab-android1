plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21"
    id("kotlin-kapt")
}

android {
    namespace = "com.remixlab.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.remixlab.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "2.5.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        // کلید AI از local.properties خونده میشه
        val deepseekKey = project.findProperty("DEEPSEEK_API_KEY") as String? ?: ""
        buildConfigField("String", "DEEPSEEK_API_KEY", "\"$deepseekKey\"")
        buildConfigField("String", "APP_VERSION", "\"$versionName\"")
        buildConfigField("boolean", "ENABLE_LOGGING", "true")
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("boolean", "ENABLE_LOGGING", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
        viewBinding = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/NOTICE.txt"
            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
    }

    // برای LAME native lib
    ndkVersion = "26.1.10909125"

    // همه معماریها برای اجرای بهتر native lib
    splits {
        abi {
            isEnable = false
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }
}

dependencies {

    // ============================================
    // 1) AndroidX Core & Lifecycle
    // ============================================
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-process:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    // ============================================
    // 2) Jetpack Compose (UI کامل)
    // ============================================
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.ui:ui-text-google-fonts")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.animation:animation-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.runtime:runtime-livedata")

    // Compose Navigation
    implementation("androidx.navigation:navigation-compose:2.8.4")

    // Accompanist (برای permission و pager و ...)
    implementation("com.google.accompanist:accompanist-permissions:0.36.0")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.36.0")
    implementation("com.google.accompanist:accompanist-pager:0.36.0")

    // Debug tools
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // ============================================
    // 3) Media3 / ExoPlayer (پخش صدا)
    // ============================================
    implementation("androidx.media3:media3-exoplayer:1.5.0")
    implementation("androidx.media3:media3-exoplayer-dash:1.5.0")
    implementation("androidx.media3:media3-exoplayer-hls:1.5.0")
    implementation("androidx.media3:media3-exoplayer-rtsp:1.5.0")
    implementation("androidx.media3:media3-datasource-okhttp:1.5.0")
    implementation("androidx.media3:media3-common:1.5.0")
    implementation("androidx.media3:media3-session:1.5.0")
    implementation("androidx.media3:media3-ui:1.5.0")
    implementation("androidx.media3:media3-transformer:1.5.0")
    implementation("androidx.media3:media3-effect:1.5.0")
    implementation("androidx.media3:media3-decoder:1.5.0")
    implementation("androidx.media3:media3-extractor:1.5.0")

    // ============================================
    // 4) Kotlin Coroutines & Flow
    // ============================================
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")

    // ============================================
    // 5) Serialization (JSON)
    // ============================================
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // ============================================
    // 6) DataStore (ذخیره تنظیمات)
    // ============================================
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.datastore:datastore-core:1.1.1")

    // ============================================
    // 7) Room Database (تاریخچه، پریستها)
    // ============================================
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // ============================================
    // 8) WorkManager (کارهای پسزمینه)
    // ============================================
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    // ============================================
    // 9) Hilt (Dependency Injection)
    // ============================================
    implementation("com.google.dagger:hilt-android:2.52")
    kapt("com.google.dagger:hilt-android-compiler:2.52")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    kapt("androidx.hilt:hilt-compiler:1.2.0")

    // ============================================
    // 10) OkHttp & Retrofit (شبکه / AI)
    // ============================================
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.11.0")
    implementation("com.google.code.gson:gson:2.11.0")

    // ============================================
    // 11) LAME MP3 Encoder (ضبط MP3)
    // ============================================
    implementation("com.github.NorthernCaptain:TAndroidLame:1.1")

    // ============================================
    // 12) Coil (بارگذاری تصویر آلبوم)
    // ============================================
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-video:2.7.0")

    // ============================================
    // 13) Vico (نمودارها و ویژوالایزر پیشرفته)
    // ============================================
    implementation("com.patrykandpatrick.vico:compose:2.0.0-alpha.1")
    implementation("com.patrykandpatrick.vico:compose-m3:2.0.0-alpha.1")

    // ============================================
    // 14) Lottie (انیمیشنهای حرفهای)
    // ============================================
    implementation("com.airbnb.android:lottie-compose:6.6.0")

    // ============================================
    // 15) Splash Screen API
    // ============================================
    implementation("androidx.core:core-splashscreen:1.0.1")

    // ============================================
    // 16) Palette (استخراج رنگ از کاور آهنگ)
    // ============================================
    implementation("androidx.palette:palette-ktx:1.0.0")

    // ============================================
    // 17) Preference (تنظیمات)
    // ============================================
    implementation("androidx.preference:preference-ktx:1.2.1")

    // ============================================
    // 18) Biometric (قفل اپ با اثر انگشت)
    // ============================================
    implementation("androidx.biometric:biometric:1.1.0")

    // ============================================
    // 19) Security Crypto (رمزنگاری پریستها)
    // ============================================
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // ============================================
    // 20) Logger (لاگ حرفهای)
    // ============================================
    implementation("com.jakewharton.timber:timber:5.0.1")

    // ============================================
    // 21) LeakCanary (فقط دیباگ - تشخیص نشتی حافظه)
    // ============================================
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")

    // ============================================
    // 22) Chucker (دیباگ شبکه - فقط دیباگ)
    // ============================================
    debugImplementation("com.github.chuckerteam.chucker:library:4.0.0")
    releaseImplementation("com.github.chuckerteam.chucker:library-no-op:4.0.0")

    // ============================================
    // 23) Showcase (راهنمای کاربر)
    // ============================================
    implementation("com.github.mreram:showcaseview:1.4.0")

    // ============================================
    // 24) Core Library Desugaring (پشتیبانی Java 8+ روی API پایین)
    // ============================================
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.2")

    // ============================================
    // 25) تست
    // ============================================
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("com.google.truth:truth:1.4.4")
    testImplementation("org.robolectric:robolectric:4.14")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
}
