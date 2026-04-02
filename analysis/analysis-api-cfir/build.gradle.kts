plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":analysis:analysis-api"))
    api(project(":analysis:analysis-api-impl-base"))
    api(project(":analysis:analysis-api-platform-interface"))
    api(project(":analysis:low-level-api-cfir"))
    implementation(project(":psi"))

    compileOnly(intellijCore())

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
