plugins {
    kotlin("jvm")
}
// Analysis API CFIR 实现（对齐 Kotlin analysis-api-fir）
dependencies {
    api(project(":analysis:analysis-api"))
    api(project(":analysis:analysis-api-impl-base"))
    api(project(":cfir:cfir-tree"))
    implementation(project(":psi"))

    compileOnly(intellijCore())

    testApi(testFixtures(project(":analysis:analysis-test-framework")))
    testApi(testFixtures(project(":tests:test-infrastructure")))
    testApi(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
