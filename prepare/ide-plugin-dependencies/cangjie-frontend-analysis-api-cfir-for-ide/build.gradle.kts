plugins {
    kotlin("jvm")
}

description = "IDE 插件依赖：打包 :analysis:analysis-api-cfir、:analysis:low-level-api-cfir、:analysis:decompiled、:analysis:symbol-light-declarations。"

publishCangjieJarsForIde(
    listOf(
        ":analysis:analysis-api-cfir",
        ":analysis:low-level-api-cfir",
        ":analysis:decompiled",
        ":analysis:decompiled:decompiler-to-file-stubs",
        ":analysis:decompiled:decompiler-to-stubs",
        ":analysis:decompiled:decompiler-to-psi",
        ":analysis:decompiled:light-declarations-for-decompiled",
        ":analysis:stubs",

        ":analysis:symbol-light-declarations",
    )
)
