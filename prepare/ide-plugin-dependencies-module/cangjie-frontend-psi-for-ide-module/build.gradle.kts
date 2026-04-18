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

configurations.configureEach {
    exclude(group = "com.jetbrains.intellij.platform")
    exclude(group = "com.jetbrains.intellij")
    exclude(group = "org.jetbrains.intellij")
    exclude(group = "com.intellij.platform")
}
