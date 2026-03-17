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

//鍩虹璁炬柦
include(":common")
include(":generators")
include(":dependencies:intellij-core")

include(":compiler")
include(":compiler:config")
include(":compiler:phaser")
include(":compiler:arguments")
include(":compiler:cli-arguments-generator")

include(":util")
//PSI 妯″潡
include(":psi")



include(":cfir")
include(":cfir:cfir-common")
include(":cfir:cfir-cones")
include(":cfir:diagnostics")
include(":cfir:symbols")
include(":cfir:resolve")
include(":cfir:cfir-tree")
include(":cfir:checkers")
include(":cfir:checkers:checkers-component-generator")
include(":cfir:cfir-serialization")
include(":cfir:entrypoint")
// RAW_CFIR: 婧愮爜 -> Raw CFIR 杞崲锛堝榻?Kotlin raw-fir锛?
include(":cfir:raw-cfir")
include(":cfir:raw-cfir:psi2cfir")
include(":cfir:raw-cfir:light-tree2cfir")
include(":cfir:raw-cfir:raw-cfir-common")

// Analysis API锛堝榻?Kotlin analysis/analysis-api锛?
include(":analysis:analysis-api")
include(":analysis:analysis-api-impl-base")
include(":analysis:analysis-api-cfir")


include(":analysis:analysis-test-framework")
include(":tests")
include(":tests:test-infrastructure")

include(":compiler:cli")
include(":compiler:chir")
include(":compiler:codegen")

include(":llvm-interop")
include(":llvm-interop:llvm-interop-api")
include(":llvm-interop:llvm-interop-jni")

include(":cfir:cfir-tree:tree-generator")
include(":flatbuffers-gen")


include(":cfir:analysis-tests")


include("cfir:diagnostic-renderers")