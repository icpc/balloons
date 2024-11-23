import nu.studer.gradle.jooq.JooqGenerate
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
version = "0.0.1"

kotlin {
    jvmToolchain(17)
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
    jooqGenerator(libs.h2)

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
    version = libs.versions.jooq

    configurations {
        create("main") {
            generateSchemaSourceOnCompilation = true
            jooqConfiguration.apply {
                logging = Logging.WARN
                jdbc.apply {
                    driver = "org.h2.Driver"
                    url = "jdbc:h2:mem:;INIT=RUNSCRIPT FROM './src/main/resources/schema.sql'"
                }
                generator.apply {
                    name = "org.jooq.codegen.KotlinGenerator"

                    database.apply {
                        inputSchema = "PUBLIC"
                        includes = ".*"
                    }
                    target.apply {
                        packageName = "org.icpclive.balloons.db"
                        directory = "./build/jooq"
                    }
                    generate.apply {
                        isKotlinNotNullPojoAttributes = true
                        isKotlinNotNullRecordAttributes = true
                        isKotlinNotNullInterfaceAttributes = true
                        isKotlinDefaultedNullablePojoAttributes = false
                        isKotlinDefaultedNullableRecordAttributes = false
                    }
                }
            }
        }
    }
}

tasks {
    named<JooqGenerate>("generateJooq") {
        inputs.file("src/main/resources/schema.sql")
        allInputsDeclared = true
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
}