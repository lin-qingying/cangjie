plugins {
    kotlin("jvm")
}

description = "IDE 插件依赖：原生补全 API、共享编辑支持和 CFIR 补全管线。"

// 明确合并三个补全模块；common/psi/analysis 由产品的既有制品提供。
publishCangjieJarsForIde(
    listOf(
        ":code-insight:completion:contracts",
        ":code-insight:completion:impl-shared",
        ":code-insight:completion:impl-cfir",
    ),
)
