plugins {
    kotlin("jvm")
}

description = "IDE 插件依赖：源码桥接原生补全 API、共享编辑支持和 CFIR 补全管线。"

dependencies {
    api(project(":code-insight:completion:contracts"))
    api(project(":code-insight:completion:impl-shared"))
    api(project(":code-insight:completion:impl-cfir"))
}

configurations.configureEach {
    exclude(group = "com.jetbrains.intellij.platform")
    exclude(group = "com.jetbrains.intellij")
    exclude(group = "org.jetbrains.intellij")
    exclude(group = "com.intellij.platform")
}
