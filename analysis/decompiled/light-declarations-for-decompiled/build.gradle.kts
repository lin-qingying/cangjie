plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(intellijCore())

    implementation(project(":analysis:analysis-api"))
    implementation(project(":analysis:analysis-api-platform-interface"))
    implementation(project(":analysis:decompiled:decompiler-to-psi"))
    implementation(project(":analysis:light-declarations"))

    testImplementation(intellijCore())
    testImplementation(testFixtures(project(":analysis:analysis-test-framework")))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
