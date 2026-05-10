plugins {
    kotlin("jvm")
    id("analysis-coverage-convention")
}

dependencies {
    compileOnly(intellijCore())

    implementation(project(":analysis:analysis-api"))
    implementation(project(":analysis:analysis-api-platform-interface"))
    implementation(project(":analysis:decompiled:decompiler-to-file-stubs"))
    implementation(project(":analysis:decompiled:decompiler-to-stubs"))
    implementation(project(":analysis:decompiled:decompiler-to-psi"))
    implementation(project(":analysis:decompiled:light-declarations-for-decompiled"))
    implementation(project(":analysis:low-level-api-cfir"))
    implementation(project(":psi"))

    testImplementation(intellijCore())
    testImplementation(project(":compiler:config"))
    testImplementation(testFixtures(project(":analysis:analysis-test-framework")))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
