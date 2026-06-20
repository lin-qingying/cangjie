plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

group = "org.cangnova.cangjie.build"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    api(project(":utilities"))
    compileOnly(libs.kotlinGradlePlugin)
}
