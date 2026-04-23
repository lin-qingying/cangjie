import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.intellij.platform.module")
}

val devEcoHome = providers.environmentVariable("DEVECO_HOME")
    .orElse(providers.gradleProperty("devEcoHome"))
val syncPlatformVersion = providers.gradleProperty("devecoSyncPlatformVersion")

kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}

dependencies {
    implementation(kotlin("stdlib"))

    intellijPlatform {
        if (devEcoHome.isPresent) {
            local(file(devEcoHome.get()))
        } else {
            intellijIdea(syncPlatformVersion.get())
        }
        composeUI()
    }
}
