plugins {
    kotlin("jvm")
}
// RAW_CFIR Common: PSI/LightTree → Raw CFIR 共享基类
dependencies {
    api(project(":cfir:cfir-tree"))
    api(project(":cfir:providers"))
    implementation(project(":psi"))

    compileOnly(intellijCore())

    testImplementation(libs.junit.jupiter)
}
