plugins {
    kotlin("jvm")
}
// 分析 API 的 CFIR 实现模块（对齐 Kotlin analysis-api-fir）
dependencies {
    api(project(":analysis:analysis-api"))
    api(project(":analysis:analysis-api-impl-base"))
    implementation(project(":cfir:entrypoint"))
    api(project(":cfir:cfir-tree"))
    api(project(":cfir:resolve"))
    api(project(":cfir:checkers"))
    api(project(":common:diagnostics"))
    implementation(project(":psi"))

    compileOnly(intellijCore())

    testImplementation(testFixtures(project(":analysis:analysis-test-framework")))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
