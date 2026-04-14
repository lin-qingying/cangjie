plugins {
    kotlin("jvm")
}

description = "IDE 插件依赖：打包 :common、:util、:compiler:arguments、:resolution.common。"

publishCangjieJarsForIde(
    listOf(
        ":common",
        ":util",
        ":analysis:cj-references",
        ":compiler:arguments",
        ":resolution.common",
    )
)
