plugins {
    kotlin("jvm")
}

description = "IDE 插件依赖：打包 :cfir:* 全系列、:common:diagnostics。"

publishCangjieJarsForIde(
    listOf(
        ":cfir:cfir-common",
        ":cfir:cfir-cones",
        ":cfir:cfir-tree",
        ":cfir:cfir-serialization",
        ":cfir:providers",
        ":cfir:resolve",
        ":cfir:semantics",
        ":compiler:config",
        ":cfir:checkers",
        ":cfir:diagnostic-renderers",
        ":cfir:raw-cfir:raw-cfir-common",
        ":cfir:raw-cfir:psi2cfir",
        ":cfir:raw-cfir:light-tree2cfir",
        ":cfir:entrypoint",
        ":common:diagnostics",
    )
)
