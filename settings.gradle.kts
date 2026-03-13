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

//基础设施
include(":common")
include(":generators")
include(":dependencies:intellij-core")

include(":compiler")
include(":compiler:config")

include(":util")
//PSI 模块
include(":psi")



include(":cfir")
include(":cfir:cfir-common")
include(":cfir:cfir-cones")
include(":cfir:diagnostics")
include(":cfir:symbols")
include(":cfir:resolve")
include(":cfir:cfir-tree")
include(":cfir:diagnostic-renderers")
include(":cfir:checkers")
include(":cfir:checkers:checkers-component-generator")
// RAW_CFIR: 源码 → Raw CFIR 转换（对齐 Kotlin raw-fir）
include(":cfir:raw-cfir")
include(":cfir:raw-cfir:psi2cfir")
include(":cfir:raw-cfir:light-tree2cfir")
include(":cfir:raw-cfir:raw-cfir-common")

// Analysis API（对齐 Kotlin analysis/analysis-api）
include(":analysis:analysis-api")
include(":analysis:analysis-api-impl-base")
include(":analysis:analysis-api-cfir")


include("analysis:analysis-test-framework")
include(":tests")
include(":tests:test-infrastructure")

include("compiler:cli")

include("cfir:cfir-tree:tree-generator")
include(":flatbuffers-gen")
