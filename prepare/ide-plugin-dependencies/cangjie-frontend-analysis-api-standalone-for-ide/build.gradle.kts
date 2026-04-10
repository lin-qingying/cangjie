plugins {
    kotlin("jvm")
}

description = "IDE 插件依赖：打包 :analysis:analysis-api-standalone、:analysis:analysis-internal-utils。"

publishCangjieJarsForIde(
    listOf(
        ":analysis:analysis-api-standalone",
        ":analysis:analysis-internal-utils",
    )
)
