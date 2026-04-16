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
    ),
    apiDependencies = listOf(
        // 在这里填写需要透传给消费方（intellij-cangjie）的三方库坐标
        // 例如：
        // "org.jetbrains.kotlin:kotlin-stdlib:2.1.0",
        // "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0",
    )
)
