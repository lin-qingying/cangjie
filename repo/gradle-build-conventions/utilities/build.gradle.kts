plugins {
    `kotlin-dsl`
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
    compileOnly(gradleApi())
    compileOnly(gradleKotlinDsl())
    implementation(libs.kotlinGradlePlugin)
}
