plugins {
    `kotlin-dsl`
    id("org.jetbrains.kotlin.jvm")
}

group = "org.cangjie.build"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    compileOnly(gradleApi())
    compileOnly(gradleKotlinDsl())
    implementation(libs.kotlinGradlePlugin)
}
