plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":chir:cfir2chir"))
    api(project(":cfir:cfir-serialization"))
    implementation(project(":flatbuffers-gen"))
    implementation(libs.flatbuffers.java)

    testImplementation(libs.junit.jupiter)
    testImplementation(project(":compiler:codegen"))
    testRuntimeOnly(libs.junit.platform.launcher)
}
