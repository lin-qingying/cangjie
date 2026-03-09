// Analysis API: 用户面向的分析 API（对齐 Kotlin analysis-api）
dependencies {
    compileOnly(intellijCore())
    implementation(project(":psi"))
    implementation(project(":cfir:cfir-tree"))
}
