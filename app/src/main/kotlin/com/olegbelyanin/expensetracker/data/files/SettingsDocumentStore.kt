package com.olegbelyanin.expensetracker.data.files

import android.content.Context
import android.content.Intent
import android.net.Uri
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

    override suspend fun writeSharedFile(fileName: String, text: String): String =
        withContext(io) {
            require(fileName.isNotBlank() && '/' !in fileName && '\\' !in fileName) { "bad-name" }
            val dir = File(context.cacheDir, SHARED_DIR).apply { mkdirs() }
            val file = File(dir, fileName)
            file.writeText(text, StandardCharsets.UTF_8)
            FileProvider.getUriForFile(context, "${context.packageName}.files", file).toString()
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

    private companion object {
        const val SHARED_DIR = "shared"
    }
}
