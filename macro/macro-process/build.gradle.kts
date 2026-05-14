plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":macro:macro-common"))

    compileOnly(intellijCore())
    implementation(libs.jna)
    implementation(libs.jna.platform)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.flatbuffers.java)
    testRuntimeOnly(libs.junit.platform.launcher)
}
