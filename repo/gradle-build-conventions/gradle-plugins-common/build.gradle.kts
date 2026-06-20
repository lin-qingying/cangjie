plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

group = "org.cangnova.cangjie.build"

repositories {
    mavenCentral()
    gradlePluginPortal()
}
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}
kotlin {
    jvmToolchain(21)
}

dependencies {
    api(project(":utilities"))
    implementation(libs.kotlinGradlePlugin)
}
