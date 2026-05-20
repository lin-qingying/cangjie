plugins {
    kotlin("jvm")
}

description = "IDE 插件依赖：源码桥接 :code-insight:folding（及其依赖的 :psi、:common、:util）。"

dependencies {
    api(project(":code-insight:folding"))
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
