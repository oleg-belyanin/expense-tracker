package com.olegbelyanin.expensetracker.seed

import java.io.File

object ProjectRoot {
    fun dir(): File = generateSequence(File(System.getProperty("user.dir")).canonicalFile) { it.parentFile }
        .first { File(it, "settings.gradle.kts").exists() && File(it, "seed-data").isDirectory }
}
