plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":flatbuffers-gen"))
    api(project(":common"))
    api(project(":cfir:cfir-tree"))
    // 宏语义展开引擎 SPI 直接暴露 cfir:providers 的构造类型；cfir 侧不依赖 macro-common，无环。
    api(project(":cfir:providers"))
    api(project(":psi"))
    implementation(libs.flatbuffers.java)

    compileOnly(intellijCore())
}
