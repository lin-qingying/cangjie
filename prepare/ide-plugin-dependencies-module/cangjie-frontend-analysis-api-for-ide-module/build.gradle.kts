plugins {
    kotlin("jvm")
}

description = "IDE 插件依赖：打包 :analysis:analysis-api、:analysis:analysis-api-platform-interface、:analysis:analysis-api-impl-base。"
dependencies {
    api(project(":analysis:analysis-api"))
    api(project(":analysis:analysis-api-platform-interface"))
    api(project(":analysis:analysis-api-impl-base"))
}