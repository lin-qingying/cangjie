plugins {
    kotlin("jvm")
}

// CFIR Symbols: 符号提供者接口与实现、Scope 管理

dependencies {
    api(project(":cfir:cfir-tree"))
    api(project(":cfir:cfir-cones"))
    api(project(":cfir:cfir-common"))
    api(project(":common"))
    api(project(":util"))
    compileOnly(intellijCore())
}
