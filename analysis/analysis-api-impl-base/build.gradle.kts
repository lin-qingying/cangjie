plugins {
    kotlin("jvm")
}
// 分析 API 基础实现模块（对齐 Kotlin analysis-api-impl-base）
dependencies {
    api(project(":analysis:analysis-api"))
    implementation(project(":psi"))
    compileOnly(intellijCore())
}
