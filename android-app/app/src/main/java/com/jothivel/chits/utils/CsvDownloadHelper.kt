package com.jothivel.chits.utils

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

object CsvDownloadHelper {
    fun save(context: Context, fileName: String, content: String): Result<String> = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/JothiVelChits")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = checkNotNull(context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)) { "Could not create download file" }
            try {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(content) }
                    ?: error("Could not write download file")
                values.clear(); values.put(MediaStore.Downloads.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
            } catch (e: Exception) {
                context.contentResolver.delete(uri, null, null); throw e
            }
            "Downloads/JothiVelChits/$fileName"
        } else {
            val dir = checkNotNull(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)).resolve("JothiVelChits").apply { mkdirs() }
            File(dir, fileName).apply { writeText(content) }.absolutePath
        }
    }
}
