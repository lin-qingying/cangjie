plugins {
    kotlin("jvm")
}

description = "IDE 插件依赖：打包 :analysis:analysis-api-cfir、:analysis:low-level-api-cfir、:analysis:decompiled、:analysis:symbol-light-declarations。"

publishCangjieJarsForIde(
    listOf(
        ":analysis:analysis-api-cfir",
        ":analysis:low-level-api-cfir",
        ":analysis:decompiled",
        ":analysis:stubs",

        ":analysis:symbol-light-declarations",
    )
)
