dependencies {
    // ... وابستگی‌های قبلی ...

    // === نسخه دوم: میکس و جداسازی ===
    // Media3 Transformer (برای CompositionPlayer و AudioMixer)
    implementation("androidx.media3:media3-transformer:1.5.0")

    // ONNX Runtime (برای اجرای مدل Demucs روی دستگاه)
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.20.0")

    // FFmpeg Kit (برای تبدیل فرمت صوتی به PCM و برعکس)
    implementation("com.arthenica:ffmpeg-kit-audio:6.0-2")

    // WorkManager (برای پردازش پس‌زمینه جداسازی)
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    // Serialization (برای ذخیره تنظیمات میکس)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}
