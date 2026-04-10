plugins {
    kotlin("jvm")
}

description = "IDE 插件依赖：打包 :analysis:analysis-api、:analysis:analysis-api-platform-interface、:analysis:analysis-api-impl-base。"

publishCangjieJarsForIde(
    listOf(
        ":analysis:analysis-api",
        ":analysis:analysis-api-platform-interface",
        ":analysis:analysis-api-impl-base",
    )
)
