plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":compiler:arguments"))

    testImplementation(project(":compiler:frontend"))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
