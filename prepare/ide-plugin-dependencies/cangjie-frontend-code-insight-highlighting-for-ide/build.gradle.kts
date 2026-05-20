plugins {
    kotlin("jvm")
}

description = "IDE 插件依赖：打包 :code-insight:highlighting（及其依赖的 :psi、:common、:util）。"

publishCangjieJarsForIde(
    listOf(
        ":code-insight:highlighting",
        ":psi",
        ":common",
        ":util",
    ),
)
