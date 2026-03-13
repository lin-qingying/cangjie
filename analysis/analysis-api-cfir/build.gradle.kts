plugins {
    kotlin("jvm")
}
// 分析 API 的 CFIR 实现模块（对齐 Kotlin analysis-api-fir）
dependencies {
    api(project(":analysis:analysis-api"))
    api(project(":analysis:analysis-api-impl-base"))
    api(project(":cfir:cfir-tree"))
    api(project(":cfir:resolve"))
    api(project(":cfir:checkers"))
    api(project(":cfir:diagnostics"))
    implementation(project(":psi"))

    compileOnly(intellijCore())

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
