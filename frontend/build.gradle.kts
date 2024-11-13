import com.github.gradle.node.pnpm.task.PnpmTask

plugins {
    alias(libs.plugins.node)
}

node {
    version.set("20.18.0")
    pnpmVersion.set("9.12.3")
    download.set(rootProject.findProperty("npm.download") == "true")
}

fun TaskContainerScope.pnpmBuild(name: String, configure: PnpmTask.(Directory) -> Unit = {}) = named<PnpmTask>(name) {
    outputs.cacheIf { true }
    environment.set(mapOf("BUILD_PATH" to "build"))
    inputs.dir(layout.projectDirectory.dir("src"))
    inputs.file(layout.projectDirectory.file("package.json"))
    inputs.file(layout.projectDirectory.file("pnpm-lock.yaml"))
    inputs.file(layout.projectDirectory.file("tsconfig.json"))
    inputs.file(layout.projectDirectory.file("tsconfig.node.json"))
    inputs.file(layout.projectDirectory.file("tsconfig.app.json"))
    inputs.file(layout.projectDirectory.file("vite.config.ts"))
    outputs.dir(layout.projectDirectory.dir("dist"))
    configure(layout.projectDirectory)
}

tasks {
    val buildApp = pnpmBuild("pnpm_run_build")
    register<Task>("build") {
        dependsOn(buildApp)
    }
}