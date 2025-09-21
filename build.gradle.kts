import org.jooq.codegen.gradle.CodegenTask
import org.jooq.meta.jaxb.Logging

plugins {
    application
    java
    alias(libs.plugins.jooq)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow)
}

group = "org.icpclive.balloons"
version = "1.0.0"

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("org.icpclive.balloons.ApplicationKt")
}

repositories {
    mavenCentral()
    maven("https://jitpack.io") {
        group = "com.github.icpc.live-v3"
    }
}

dependencies {
    jooqCodegen(libs.h2)

    compileOnly(libs.live.schemas)

    implementation(libs.bundles.ktor)
    implementation(libs.logback)
    implementation(libs.live.cds)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.h2)
    implementation(libs.bundles.jooq)
    implementation(libs.bcrypt)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
}

jooq {
    configuration {
        logging = Logging.WARN

        jdbc {
            driver = "org.h2.Driver"
            url = "jdbc:h2:mem:;INIT=RUNSCRIPT FROM './src/main/resources/schema.sql'"
        }

        generator {
            name = "org.jooq.codegen.KotlinGenerator"

            database {
                inputSchema = "PUBLIC"
                includes = ".*"
            }

            target {
                directory = "build/jooq"
                packageName = "org.icpclive.balloons.db"
            }

            generate {
                isKotlinNotNullPojoAttributes = true
                isKotlinNotNullRecordAttributes = true
                isKotlinNotNullInterfaceAttributes = true
                isKotlinDefaultedNullablePojoAttributes = false
                isKotlinDefaultedNullableRecordAttributes = false
            }
        }
    }
}

tasks.compileKotlin {
    dependsOn(tasks.jooqCodegen)
}

tasks.register<Exec>("downloadLive") {
    description = "Downloads ICPC Live repository"
    group = "release"

    val repoUrl = "https://github.com/icpc/live-v3.git"
    // FIXME after live release
    // val tag = "v" + libs.versions.live.get()
    val targetDirectory = "build/live"

    doFirst {
        delete(targetDirectory)
    }

    commandLine(
        "git", "clone", "--depth", "1",
        // "--branch", tag,
        repoUrl, targetDirectory
    )

    workingDir = projectDir

    outputs.dir(targetDirectory)
    outputs.cacheIf { true }
}

tasks {
    named<CodegenTask>("jooqCodegen") {
        inputs.file("src/main/resources/schema.sql")
    }

    named<Test>("test") {
        useJUnitPlatform()
    }

    shadowJar {
        mergeServiceFiles()
        archiveClassifier = null
    }

    processResources {
        if (project.properties["balloons.embedFrontend"] == "true") {
            from(project(":frontend").tasks.named("pnpm_run_build")) {
                into("frontend")
            }
        }
    }

    val emptyJson by registering {
        val file = project.layout.buildDirectory.file("empty.json")
        outputs.file(file)
        doLast {
            file.get().asFile.writeText("{\n}\n")
        }
    }

    val emptyJsonArray by registering {
        val file = project.layout.buildDirectory.file("empty-array.json")
        outputs.file(file)
        doLast {
            file.get().asFile.writeText("[\n]\n")
        }
    }

    val balloonsArchive = register<Sync>("balloonsArchive") {
        group = "release"
        dependsOn("downloadLive")
        destinationDir = project.layout.buildDirectory.dir("archive").get().asFile

        val liveDir = project.layout.buildDirectory.dir("live").get().asFile
        val configDir = File(liveDir, "config")
        val userArchiveDir = File(liveDir, "src/user-archive")

        from(File(configDir, "_examples")) {
            includeEmptyDirs = false
            include("codeforces/**")
            include("clics/**")
            include("cms/**")
            include("pcms/**")
            include("yandex/**")
            into("examples/config")
        }

        from(configDir) {
            into("examples")
            include("creds.json.example")
            rename { it.removeSuffix(".example") }
        }

        from(project.tasks.shadowJar) {
            rename { "balloons.jar" }
        }

        val vscodeDir = File(userArchiveDir, "vscode")
        if (vscodeDir.exists()) {
            from(vscodeDir) {
                into(".vscode")
            }
        }

        val schemasJar = configurations.compileClasspath.get().find { it.name.contains("org.icpclive.cds.schemas") }
        if (schemasJar != null) {
            from(zipTree(schemasJar)) {
                include("schemas/*.schema.json")
                into(".vscode")
            }
        }

        // Helper function for empty JSON files
        fun emptyJson(dir: String, name: String, task: TaskProvider<Task> = emptyJson) = from(task) {
            into(dir)
            rename { "$name.json" }
        }

        emptyJson("config", "settings")
        emptyJson("config", "advanced", emptyJsonArray)
        emptyJson("", "creds")

        from("release-archive")
    }

    register<Zip>("release") {
        group = "release"
        dependsOn(balloonsArchive)
        from(balloonsArchive)
        archiveFileName = "balloons-${version}.zip"
        destinationDirectory = project.layout.buildDirectory.dir("artifacts")

        doFirst {
            mkdir("artifacts")
        }
    }
}