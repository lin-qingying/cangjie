plugins {
    kotlin("jvm")
    id("analysis-coverage-convention")
}

dependencies {
    compileOnly(intellijCore())

    implementation(project(":analysis:analysis-api"))
    implementation(project(":analysis:analysis-api-platform-interface"))
    implementation(project(":analysis:analysis-internal-utils"))
    implementation(project(":analysis:light-declarations"))
    implementation(project(":analysis:decompiled:light-declarations-for-decompiled"))
    implementation(project(":analysis:stubs"))
    implementation(project(":psi"))
    implementation(project(":common"))

    testImplementation(testFixtures(project(":analysis:analysis-test-framework")))
    testImplementation(project(":analysis:analysis-api-cfir"))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
