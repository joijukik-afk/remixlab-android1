plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
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
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE*"
            excludes += "/META-INF/NOTICE*"
        }
    }
}

dependencies {

    // ============================================
    // AndroidX Core & Lifecycle
    // ============================================
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    // ============================================
    // Jetpack Compose
    // ============================================
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // ============================================
    // Media3 / ExoPlayer
    // ============================================
    implementation("androidx.media3:media3-exoplayer:1.5.0")
    implementation("androidx.media3:media3-common:1.5.0")
    implementation("androidx.media3:media3-session:1.5.0")
    implementation("androidx.media3:media3-ui:1.5.0")

    // ============================================
    // Kotlin Coroutines
    // ============================================
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

    // ============================================
    // DataStore (ذخیره پریست)
    // ============================================
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // ============================================
    // OkHttp (برای AI)
    // ============================================
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // ============================================
    // LAME MP3 Encoder (ضبط MP3)
    // ============================================
    implementation("com.github.NorthernCaptain:TAndroidLame:1.1")

    // ============================================
    // Coil (بارگذاری کاور آلبوم)
    // ============================================
    implementation("io.coil-kt:coil-compose:2.7.0")

    // ============================================
    // تست
    // ============================================
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
