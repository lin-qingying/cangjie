plugins {
    kotlin("jvm")
//    `java-library`
}

description = "IDE 插件依赖：打包 :psi（及其依赖的 :common、:util）。"


dependencies {
    api(project(":psi"))
    api(project(":common"))
    api(project(":util"))
}
