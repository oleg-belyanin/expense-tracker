package com.olegbelyanin.expensetracker.domain.backup

import kotlinx.serialization.json.Json
import java.time.Instant

object BackupJson {
    val format: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    fun encode(snapshot: BackupSnapshot): String {
        val file = BackupFile(
            format = BackupFile.FORMAT,
            formatVersion = BackupFile.FORMAT_VERSION,
            schemaVersion = snapshot.schemaVersion,
            normalizerVersion = snapshot.normalizerVersion,
            seedDataVersion = snapshot.seedDataVersion,
            exportedAt = Instant.ofEpochMilli(snapshot.exportedAtEpochMs).toString(),
            categories = snapshot.categories,
            locations = snapshot.locations,
            expenses = snapshot.expenses,
            keywords = snapshot.keywords,
            exactRules = snapshot.exactRules,
            nameContexts = snapshot.nameContexts,
            learningExamples = snapshot.learningExamples,
            transitions = snapshot.transitions,
            keywordStats = snapshot.keywordStats,
            locationStats = snapshot.locationStats,
        )
        return format.encodeToString(BackupFile.serializer(), file)
    }

    fun decode(text: String): BackupFile {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) throw BackupCorruptedException()
        return try {
            format.decodeFromString(BackupFile.serializer(), trimmed)
        } catch (_: Exception) {
            throw BackupCorruptedException()
        }
    }

    fun snapshotOf(file: BackupFile): BackupSnapshot {
        val exportedAt = try {
            Instant.parse(file.exportedAt).toEpochMilli()
        } catch (_: Exception) {
            throw BackupCorruptedException()
        }
        return file.toSnapshot(exportedAt)
    }

    fun decodeSnapshot(text: String): BackupSnapshot = snapshotOf(decode(text))
}
