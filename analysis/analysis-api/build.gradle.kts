
plugins {
    kotlin("jvm")
}
// 面向用户的分析 API 模块（对齐 Kotlin analysis-api）
dependencies {
    compileOnly(intellijCore())
    implementation(project(":psi"))
    implementation(project(":cfir:cfir-tree"))
}
