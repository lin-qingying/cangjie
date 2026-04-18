

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    kotlin("jvm")
}



repositories {
    mavenCentral()
    gradlePluginPortal()


}

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalBuildToolsApi::class)
    jvmToolchain(21)

    compilerOptions {
        allWarningsAsErrors.set(true)
        optIn.add("kotlin.ExperimentalStdlibApi")
    }
}

dependencies {
}

