plugins {
    kotlin("jvm")
}

// CFIR Serialization: .cjo 文件反序列化，跨模块符号加载

dependencies {
    api(project(":cfir:cfir-common"))
    api(project(":cfir:cfir-cones"))
    api(project(":cfir:cfir-tree"))
    api(project(":cfir:providers"))
    implementation(project(":flatbuffers-gen"))
    implementation(project(":common"))
    implementation(project(":util"))
    implementation(libs.flatbuffers.java)
    api(project(":psi"))
    compileOnly(intellijCore())

    testImplementation(testFixtures(project(":tests:test-infrastructure")))
    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
