plugins {
    kotlin("jvm")
}

description = "IDE 插件依赖：源码桥接 code-insight 的契约与全部编辑器能力模块。"

dependencies {
    api(project(":code-insight:api"))
    api(project(":code-insight:editor"))
    api(project(":code-insight:highlighting"))
    api(project(":code-insight:completion"))
    api(project(":code-insight:code-actions"))
    api(project(":code-insight:hints"))
    api(project(":code-insight:refactoring"))
    api(project(":psi"))
    api(project(":common"))
    api(project(":util"))
    api(project(":analysis:analysis-api"))
    api(project(":analysis:analysis-api-cfir"))
}

configurations.configureEach {
    exclude(group = "com.jetbrains.intellij.platform")
    exclude(group = "com.jetbrains.intellij")
    exclude(group = "org.jetbrains.intellij")
    exclude(group = "com.intellij.platform")
}
