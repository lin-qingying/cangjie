
plugins {
    kotlin("jvm")
}
// RAW_CFIR LightTree2CFIR: LightTree → Raw CFIR 转换（对齐 Kotlin raw-fir/light-tree2fir）
dependencies {
    api(project(":cfir:cfir-tree"))
    api(project(":cfir:raw-cfir:raw-cfir-common"))

    compileOnly(intellijCore())

    testImplementation(libs.junit.jupiter)
}
