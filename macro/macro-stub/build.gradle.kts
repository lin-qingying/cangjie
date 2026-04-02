plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":macro:macro-common"))

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
