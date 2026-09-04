plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:categorization"))
    implementation(project(":core:domain"))
    testImplementation(libs.junit)
}

tasks.register<JavaExec>("generateSeed") {
    group = "seed"
    description = "Generate seed artifact into app/src/main/assets/seed/"
    dependsOn(tasks.named("classes"))
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.olegbelyanin.expensetracker.seed.SeedDataGeneratorKt")
    val root = rootProject.layout.projectDirectory
    args(
        "--train",
        root.file("seed-data/raw/train.csv").asFile.absolutePath,
        "--validation",
        root.file("seed-data/raw/validation.csv").asFile.absolutePath,
        "--output",
        root.file("app/src/main/assets/seed/").asFile.absolutePath,
        "--config",
        root.file("seed-data/categorization-config.json").asFile.absolutePath,
        "--categories",
        root.file("seed-data/categories.yaml").asFile.absolutePath,
        "--report",
        root.file("seed-data/reports/seed-v1-validation.json").asFile.absolutePath,
    )
}

tasks.register<JavaExec>("generateDemo") {
    group = "demo"
    description = "Generate UI demo expenses CSV (not seed) into demo-data/ and debug assets"
    dependsOn(tasks.named("classes"))
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.olegbelyanin.expensetracker.seed.DemoDataGeneratorKt")
    val root = rootProject.layout.projectDirectory
    args(
        "--train",
        root.file("seed-data/raw/train.csv").asFile.absolutePath,
        "--count",
        "300",
        "--prefix",
        "ui",
        "--output",
        root.file("demo-data/expenses-ui.csv").asFile.absolutePath,
        "--output",
        root.file("app/src/debug/assets/demo/expenses.csv").asFile.absolutePath,
    )
}

tasks.register<JavaExec>("generateNfrDemo") {
    group = "demo"
    description = "Generate local 5000-row dump for NFR-2; do not commit"
    dependsOn(tasks.named("classes"))
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.olegbelyanin.expensetracker.seed.DemoDataGeneratorKt")
    val root = rootProject.layout.projectDirectory
    args(
        "--train",
        root.file("seed-data/raw/train.csv").asFile.absolutePath,
        "--count",
        "5000",
        "--prefix",
        "nfr",
        "--output",
        root.file("demo-data/local/expenses-nfr-5000.csv").asFile.absolutePath,
    )
}
