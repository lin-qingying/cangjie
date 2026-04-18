plugins {
    kotlin("jvm")
}

description = "IDE 插件依赖：打包 :analysis:analysis-api-standalone、:analysis:analysis-internal-utils。"
dependencies {
    api(project(":analysis:analysis-api-standalone"))
    api(project(":analysis:analysis-internal-utils"))
}

configurations.configureEach {
    exclude(group = "com.jetbrains.intellij.platform")
    exclude(group = "com.jetbrains.intellij")
    exclude(group = "org.jetbrains.intellij")
    exclude(group = "com.intellij.platform")
}
