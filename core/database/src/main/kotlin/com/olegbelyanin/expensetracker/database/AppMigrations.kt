package com.olegbelyanin.expensetracker.database

import androidx.room3.migration.Migration

/**
 * Явный список миграций Room (F-08).
 *
 * Версия 1 — первая схема из §7 AD-CAT-001, экспорт в `schemas/`.
 * Следующий релиз: добавить `Migration(1, 2)` сюда, поднять
 * [AppDatabase.SCHEMA_VERSION] и `@Database(version)`, закоммитить `2.json`.
 *
 * `fallbackToDestructiveMigration` не используем: неизвестная версия
 * должна упасть, а не стереть расходы пользователя.
 */
object AppMigrations {
    val all: Array<Migration> = emptyArray()
}
