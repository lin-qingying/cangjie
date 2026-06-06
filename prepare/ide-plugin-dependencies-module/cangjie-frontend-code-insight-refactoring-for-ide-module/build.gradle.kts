plugins {
    kotlin("jvm")
}

description = "IDE 插件依赖：源码桥接 :code-insight:refactoring（及其依赖的 :psi、:common、:util）。"

dependencies {
    api(project(":code-insight:refactoring"))
    api(project(":psi"))
    api(project(":common"))
    api(project(":util"))
    compileOnly("com.jetbrains.intellij.platform:analysis:${property("intellijSdkVersion")}") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.platform:refactoring:${property("intellijSdkVersion")}") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.platform:usage-view:${property("intellijSdkVersion")}") { isTransitive = false }
}

configurations.configureEach {
    exclude(group = "com.jetbrains.intellij.platform")
    exclude(group = "com.jetbrains.intellij")
    exclude(group = "org.jetbrains.intellij")
    exclude(group = "com.intellij.platform")
}
