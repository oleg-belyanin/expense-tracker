// AGP 9 includes Kotlin. A newer KGP than AGP's baseline is declared here
// so Version Catalog stays the only place versions are pinned.
buildscript {
    dependencies {
        classpath(libs.kotlin.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.ktlint)
}

val ktlintVersion = libs.versions.ktlint.get()

configureKtlint()

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    configureKtlint()
}

fun org.gradle.api.Project.configureKtlint() {
    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set(ktlintVersion)
        android.set(true)
        ignoreFailures.set(false)
        filter {
            exclude("**/generated/**")
            exclude("**/build/**")
            exclude("**/org/tartarus/snowball/**")
        }
    }
}
