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
    implementation(project(":psi"))
    implementation(project(":cfir:cfir-common"))
    implementation(project(":cfir:cfir-tree"))
    implementation(project(":cfir:cfir-serialization"))
    implementation(project(":flatbuffers-gen"))
    implementation(libs.flatbuffers.java)

    testImplementation(intellijCore())
    testImplementation(testFixtures(project(":analysis:analysis-test-framework")))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
