plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":macro:macro-common"))

    compileOnly(intellijCore())

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.flatbuffers.java)
    testRuntimeOnly(libs.junit.platform.launcher)
}
