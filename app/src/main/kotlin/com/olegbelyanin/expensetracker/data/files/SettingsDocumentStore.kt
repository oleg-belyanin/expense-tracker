package com.olegbelyanin.expensetracker.data.files

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.charset.StandardCharsets

interface SettingsDocumentStore {
    suspend fun writeText(uri: String, text: String)

    suspend fun writeSharedFile(fileName: String, text: String): String

    suspend fun readText(uri: String): String
}

class AndroidSettingsDocumentStore(
    private val context: Context,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : SettingsDocumentStore {
    private val resolver = context.contentResolver

    override suspend fun writeText(uri: String, text: String) {
        withContext(io) {
            val parsed = Uri.parse(uri)
            resolver.openOutputStream(parsed, "wt")?.use { stream ->
                stream.writer(StandardCharsets.UTF_8).use { writer ->
                    writer.write(text)
                }
            } ?: error("missing-output")
            takePersistableRead(parsed)
        }
    }

    override suspend fun writeSharedFile(fileName: String, text: String): String = withContext(io) {
        require(fileName.isNotBlank() && '/' !in fileName && '\\' !in fileName) { "bad-name" }
        writeToDownloads(fileName, text) ?: writeToCache(fileName, text)
    }

    private fun writeToDownloads(fileName: String, text: String): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val values =
            ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType(fileName))
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
        return try {
            resolver.openOutputStream(uri, "wt")?.use { stream ->
                stream.writer(StandardCharsets.UTF_8).use { writer ->
                    writer.write(text)
                }
            } ?: error("missing-output")
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri.toString()
        } catch (_: Exception) {
            resolver.delete(uri, null, null)
            null
        }
    }

    private fun writeToCache(fileName: String, text: String): String {
        val dir = File(context.cacheDir, SHARED_DIR).apply { mkdirs() }
        val file = File(dir, fileName)
        file.writeText(text, StandardCharsets.UTF_8)
        return FileProvider.getUriForFile(context, "${context.packageName}.files", file).toString()
    }

    override suspend fun readText(uri: String): String = withContext(io) {
        val parsed = Uri.parse(uri)
        resolver.openInputStream(parsed)?.bufferedReader(StandardCharsets.UTF_8)?.use { reader ->
            reader.readText()
        } ?: error("missing-input")
    }

    private fun takePersistableRead(uri: Uri) {
        try {
            resolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        } catch (_: SecurityException) {
        } catch (_: IllegalArgumentException) {
        }
    }

    private fun mimeType(fileName: String): String = when {
        fileName.endsWith(".json", ignoreCase = true) -> "application/json"
        fileName.endsWith(".csv", ignoreCase = true) -> "text/csv"
        else -> "application/octet-stream"
    }

    private companion object {
        const val SHARED_DIR = "shared"
    }
}
