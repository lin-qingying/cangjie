plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    id("org.jetbrains.kotlin.jvm")
}



repositories {
    mavenCentral()
    gradlePluginPortal()
}

kotlin {
    jvmToolchain(17)
}

java {
    disableAutoTargetJvm()
}

dependencies {
    api(project(":gradle-plugins-common"))
    implementation(libs.kotlinGradlePlugin)
}

tasks.validatePlugins.configure {
    enabled = false
}
