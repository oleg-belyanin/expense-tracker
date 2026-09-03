// AGP 9 includes Kotlin. A newer KGP than AGP's baseline is declared here
// so Version Catalog stays the only place versions are pinned.
buildscript {
    dependencies {
        classpath(libs.kotlin.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
