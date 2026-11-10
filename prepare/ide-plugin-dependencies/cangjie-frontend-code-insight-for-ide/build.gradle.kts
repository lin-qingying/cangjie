plugins {
    kotlin("jvm")
}

description = "IDE 插件依赖：打包 code-insight 的契约与全部编辑器能力模块。"

publishCangjieJarsForIde(
    listOf(
        ":code-insight:api",
        ":code-insight:editor",
        ":code-insight:highlighting",
        ":code-insight:completion",
        ":code-insight:code-actions",
        ":code-insight:hints",
        ":code-insight:refactoring",
        ":psi",
        ":common",
        ":util",
        ":analysis:analysis-api",
        ":analysis:analysis-api-cfir",
    ),
)
