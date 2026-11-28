plugins {
    kotlin("jvm")
}

description = "IDE 插件依赖：打包 :cfir:* 全系列、:compiler:frontend、:compiler:config、:common:diagnostics。"



dependencies {
    // 项目依赖
    api(project(":cfir:cfir-common"))
    api(project(":cfir:cfir-cones"))
    api(project(":cfir:cfir-tree"))
    api(project(":cfir:cfir-serialization"))
    api(project(":cfir:providers"))
    api(project(":cfir:resolve"))
    api(project(":cfir:semantics"))
    api(project(":compiler:config"))
    api(project(":cfir:checkers"))
    api(project(":cfir:diagnostic-renderers"))
    api(project(":cfir:raw-cfir:raw-cfir-common"))
    api(project(":cfir:raw-cfir:psi2cfir"))
    api(project(":cfir:raw-cfir:light-tree2cfir"))
    api(project(":cfir:entrypoint"))
    // IDE Analysis 与主编译器共享 MacroConstructionService 的 fragment parse 与 stable splice 实现。
    api(project(":compiler:frontend"))
    api(project(":common:diagnostics"))

    // 第三方库依赖（如果原 apiDependencies 中有内容，请在此处添加）
    // 示例：
    // api("org.jetbrains.kotlin:kotlin-stdlib:2.1.0")
    // api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
}

configurations.configureEach {
    exclude(group = "com.jetbrains.intellij.platform")
    exclude(group = "com.jetbrains.intellij")
    exclude(group = "org.jetbrains.intellij")
    exclude(group = "com.intellij.platform")
}
