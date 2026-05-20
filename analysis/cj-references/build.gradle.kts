plugins {
    kotlin("jvm")
    id("analysis-coverage-convention")
}

dependencies {
    compileOnly(intellijCore())

    api(project(":analysis:analysis-api"))
    api(project(":analysis:analysis-api-platform-interface"))
    api(project(":analysis:analysis-internal-utils"))
    implementation(project(":common"))
    implementation(project(":psi"))

    testImplementation(intellijCore())
    testImplementation(project(":analysis:analysis-api-cfir"))
    testImplementation(project(":analysis:analysis-api-standalone"))
    testImplementation(testFixtures(project(":analysis:analysis-test-framework")))
    testImplementation(testFixtures(project(":analysis:analysis-api-standalone")))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
