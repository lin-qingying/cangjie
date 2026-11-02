plugins {
    kotlin("jvm")
    `java-test-fixtures`
}

dependencies {
    compileOnly(intellijCore())

    implementation(project(":analysis:analysis-api"))
    implementation(project(":analysis:analysis-api-platform-interface"))
    implementation(project(":analysis:decompiled:decompiler-to-file-stubs"))
    implementation(project(":analysis:decompiled:decompiler-to-stubs"))
    implementation(project(":common"))
    implementation(project(":util"))
    implementation(project(":psi"))
    implementation(project(":cfir:cfir-common"))
    implementation(project(":cfir:cfir-tree"))
    implementation(project(":cfir:cfir-serialization"))
    testImplementation(project(":cfir:resolve"))
    implementation(project(":flatbuffers-gen"))
    implementation(libs.flatbuffers.java)

    testImplementation(intellijCore())
    testImplementation(testFixtures(project(":analysis:analysis-test-framework")))
    testImplementation(testFixtures(project(":analysis:analysis-api-standalone")))
    testImplementation(testFixtures(project(":cfir:analysis-tests")))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
