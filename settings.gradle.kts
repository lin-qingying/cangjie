pluginManagement {
    includeBuild("repo/gradle-settings-conventions")
    includeBuild("repo/gradle-build-conventions")

    repositories {
        maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("jvm-toolchain-provisioning")
}

rootProject.name = "cangjie"

// 基础设施
include(":common")
include(":generators")
include(":dependencies:intellij-core")

include(":compiler")
include(":compiler:config")
include(":compiler:phaser")
include(":compiler:arguments")
include(":compiler:frontend-arguments-generator")
include(":compiler:frontend")
include(":compiler:cli:cli-base")
include(":compiler:cli")
include(":prepare:frontend")
include(":prepare:frontend-embeddable")
include(":prepare:test-infrastructure")
include(":prepare:analysis-test-framework")

// IDE 插件依赖（按功能分组的 fat jar，对齐 Kotlin prepare/ide-plugin-dependencies）
include(":prepare:ide-plugin-dependencies:cangjie-frontend-common-for-ide")
include(":prepare:ide-plugin-dependencies:cangjie-frontend-psi-for-ide")
include(":prepare:ide-plugin-dependencies:cangjie-frontend-code-insight-for-ide")
include(":prepare:ide-plugin-dependencies:cangjie-frontend-cfir-for-ide")
include(":prepare:ide-plugin-dependencies:cangjie-frontend-analysis-api-for-ide")
include(":prepare:ide-plugin-dependencies:cangjie-frontend-analysis-api-cfir-for-ide")
include(":prepare:ide-plugin-dependencies:cangjie-frontend-analysis-api-standalone-for-ide")



include("prepare:ide-plugin-dependencies-module")
include(":prepare:ide-plugin-dependencies-module:cangjie-frontend-psi-for-ide-module")
include(":prepare:ide-plugin-dependencies-module:cangjie-frontend-common-for-ide-module")
include(":prepare:ide-plugin-dependencies-module:cangjie-frontend-code-insight-for-ide-module")

include(":prepare:ide-plugin-dependencies-module:cangjie-frontend-cfir-for-ide-module")
include(":prepare:ide-plugin-dependencies-module:cangjie-frontend-analysis-api-for-ide-module")
include(":prepare:ide-plugin-dependencies-module:cangjie-frontend-analysis-api-cfir-for-ide-module")
include(":prepare:ide-plugin-dependencies-module:cangjie-frontend-analysis-api-standalone-for-ide-module")


include(":util")
include(":gradle-queue-cli") // 跨进程 gradle 调用排队器，零内部依赖
include(":lsp")
// PSI 模块
include(":psi")

// Code Insight，共享 IDE 与 LSP 的编辑器能力。
// 一个用户可见能力 = 一个模块；能力内部的契约/机制/流水线用包分层，不再拆模块。
//   api         —— 对外扩展契约（quick-fix 注册、成员生成、补全服务与插入/导入计划）
//   editor      —— 编辑力学：折叠、格式化与代码风格
//   highlighting—— 高亮：词法/结构/语义着色、属性 key、CDoc 渲染
//   completion  —— 补全：contributor、section、lookup、排序、插入、恢复、统计
//   code-actions—— 诊断驱动的代码修改：quick fixes、override/implement
//   hints       —— inlay hints：类型、参数名、lambda、调用链、注解参数
//   refactoring —— 重命名与重构
include(":code-insight:api")
include(":code-insight:editor")
include(":code-insight:highlighting")
include(":code-insight:completion")
include(":code-insight:code-actions")
include(":code-insight:hints")
include(":code-insight:refactoring")



include(":cfir")
include(":cfir:cfir-common")

include(":cfir:semantics")
include(":cfir:cfir-cones")
include(":common:diagnostics")
include(":cfir:resolve")
include(":cfir:cfir-tree")
include(":cfir:checkers")
include(":cfir:checkers:checkers-component-generator")
include(":cfir:cfir-serialization")
include(":cfir:entrypoint")
// RAW_CFIR: 源码 -> Raw CFIR 转换，对齐 Kotlin raw-fir
include(":cfir:raw-cfir")
include(":cfir:raw-cfir:psi2cfir")
include(":cfir:raw-cfir:light-tree2cfir")
include(":cfir:raw-cfir:raw-cfir-common")

// Analysis API，对齐 Kotlin analysis/analysis-api
include(":analysis:analysis-api")
include(":analysis:analysis-api-platform-interface")
include(":analysis:analysis-api-impl-base")
include(":analysis:analysis-api-standalone")
include(":analysis:analysis-api-cfir")
include(":analysis:analysis-api-cfir:analysis-api-cfir-generator")
include(":analysis:low-level-api-cfir")
// 宏语义展开的宿主无关编排：环境 SPI、服务与触发（Analysis 层一等能力）
include(":analysis:macro-expansion")
include(":analysis:analysis-internal-utils")
include(":analysis:cj-references")
include(":analysis:stubs")
include(":analysis:decompiled")
include(":analysis:decompiled:decompiler-to-file-stubs")
include(":analysis:decompiled:decompiler-to-stubs")
include(":analysis:decompiled:decompiler-to-psi")
include(":analysis:decompiled:light-declarations-for-decompiled")

include(":analysis:light-declarations")

include(":analysis:symbol-light-declarations")
include(":analysis:analysis-tools")


include(":analysis:analysis-test-framework")
// 真实项目对照测试：先用 cjpm 编译确认真实项目无错误，再在同一项目上运行 CFIR 分析
include(":analysis:analysis-real-project-test")

// Analysis API 性能/统计测试入口：独立模块承载 OpenTelemetry SDK 与统计断言基类
include(":analysis:analysis-performance-test")
include(":tests")
include(":tests:test-infrastructure")

include(":chir")
include(":chir:chir-tree")
include(":chir:cfir2chir")
include(":compiler:codegen")
include(":compiler:jvm-codegen")

include(":llvm-interop")
include(":llvm-interop:llvm-interop-api")
include(":llvm-interop:llvm-interop-jni")

include(":cfir:cfir-tree:tree-generator")
include(":flatbuffers-gen")


include(":cfir:analysis-tests")

// 宏展开模块
include(":macro:macro-common")
include(":macro:macro-process")
include(":macro:macro-stub")


include("cfir:diagnostic-renderers")

include("compiler:plugin")

include("cfir:providers")
include("resolution.common")



