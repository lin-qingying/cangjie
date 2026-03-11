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

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
