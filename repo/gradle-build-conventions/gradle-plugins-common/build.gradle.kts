plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    id("org.jetbrains.kotlin.jvm")
}

group = "org.cangnova.cangjie.build"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(project(":utilities"))
    implementation(libs.kotlinGradlePlugin)
}
