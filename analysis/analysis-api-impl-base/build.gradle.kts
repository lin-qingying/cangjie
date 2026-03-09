// Analysis API 基础实现（对齐 Kotlin analysis-api-impl-base）
dependencies {
    api(project(":analysis:analysis-api"))
    implementation(project(":psi"))
    compileOnly(intellijCore())
}
