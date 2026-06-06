plugins {
    kotlin("jvm")
}

description = "IDE 插件依赖：打包 :code-insight:refactoring（及其依赖的 :psi、:common、:util）。"

publishCangjieJarsForIde(
    listOf(
        ":code-insight:refactoring",
        ":psi",
        ":common",
        ":util",
    ),
)
