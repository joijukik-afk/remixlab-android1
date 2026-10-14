private fun saveToMediaStore(tempFile: File) {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val fileName = "RemixLab_$timestamp.m4a"

    val values = ContentValues().apply {
        put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
        put(MediaStore.Audio.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
        put(MediaStore.Audio.Media.IS_MUSIC, 1)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/RemixLab")
            put(MediaStore.Audio.Media.IS_PENDING, 1) // علامتگذاری به عنوان در حال نوشتن
        }
    }

    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    } else {
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    }

    val uri = context.contentResolver.insert(collection, values)
    if (uri == null) {
        onError?.invoke("خطا در ایجاد فایل در MediaStore")
        return
    }

    try {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            FileInputStream(tempFile).use { inputStream ->
                inputStream.copyTo(outputStream)
            }
        }

        // پس از نوشتن، IS_PENDING را صفر کن تا فایل برای سایر برنامهها قابل مشاهده شود
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Audio.Media.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
        }

        tempFile.delete()
        onFinish?.invoke(uri)

    } catch (e: Exception) {
        e.printStackTrace()
        context.contentResolver.delete(uri, null, null) // حذف فایل ناقص
        onError?.invoke("خطا در ذخیره فایل: ${e.message}")
    }
}
