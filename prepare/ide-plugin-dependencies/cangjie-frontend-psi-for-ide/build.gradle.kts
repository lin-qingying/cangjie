plugins {
    kotlin("jvm")
}

description = "IDE 插件依赖：打包 :psi（及其依赖的 :common、:util）。"

publishCangjieJarsForIde(
    listOf(
        ":psi",
        ":common",
        ":util",
    )
)
