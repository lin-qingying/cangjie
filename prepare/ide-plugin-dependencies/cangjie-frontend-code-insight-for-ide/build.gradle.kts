plugins {
    kotlin("jvm")
}

description = "IDE 插件依赖：打包 code-insight quick-fix API、K2 fixes 与 override/implement 子模块。"

publishCangjieJarsForIde(
    listOf(
        ":code-insight:api",
        ":code-insight:fixes",
        ":code-insight:override-implement",
        ":analysis:analysis-api",
        ":analysis:analysis-api-cfir",
    ),
)
