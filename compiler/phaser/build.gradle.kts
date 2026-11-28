plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":compiler:config"))

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
