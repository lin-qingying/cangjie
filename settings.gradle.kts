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
include(":prepare:frontend")
include(":prepare:frontend-embeddable")
include(":prepare:test-infrastructure")
include(":prepare:analysis-test-framework")

// IDE 插件依赖（按功能分组的 fat jar，对齐 Kotlin prepare/ide-plugin-dependencies）
include(":prepare:ide-plugin-dependencies:cangjie-frontend-common-for-ide")
include(":prepare:ide-plugin-dependencies:cangjie-frontend-psi-for-ide")
include(":prepare:ide-plugin-dependencies:cangjie-frontend-cfir-for-ide")
include(":prepare:ide-plugin-dependencies:cangjie-frontend-analysis-api-for-ide")
include(":prepare:ide-plugin-dependencies:cangjie-frontend-analysis-api-cfir-for-ide")
include(":prepare:ide-plugin-dependencies:cangjie-frontend-analysis-api-standalone-for-ide")

include(":util")
include(":lsp")
// PSI 模块
include(":psi")



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
include(":analysis:low-level-api-cfir")
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
include(":tests")
include(":tests:test-infrastructure")

include(":compiler:chir")
include(":compiler:codegen")

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

include("intellij-ide")
