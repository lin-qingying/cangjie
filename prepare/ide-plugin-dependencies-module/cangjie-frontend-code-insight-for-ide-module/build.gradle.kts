plugins {
    kotlin("jvm")
}

description = "IDE 插件依赖：源码桥接 code-insight quick-fix API、K2 fixes 与 override/implement 子模块。"

dependencies {
    api(project(":code-insight:api"))
    api(project(":code-insight:fixes-k2"))
    api(project(":code-insight:override-implement-k2"))
    api(project(":analysis:analysis-api"))
    api(project(":analysis:analysis-api-cfir"))
}

configurations.configureEach {
    exclude(group = "com.jetbrains.intellij.platform")
    exclude(group = "com.jetbrains.intellij")
    exclude(group = "org.jetbrains.intellij")
    exclude(group = "com.intellij.platform")
}
