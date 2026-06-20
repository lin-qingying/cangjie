plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}



repositories {
    mavenCentral()
    gradlePluginPortal()
}

kotlin {
    jvmToolchain(21)
}

java {
    disableAutoTargetJvm()
}

dependencies {
    api(project(":utilities"))
    api(project(":gradle-plugins-common"))
    implementation(libs.kotlinGradlePlugin)
    implementation(libs.shadowGradlePlugin)
}

tasks.validatePlugins.configure {
    enabled = false
}
