package org.cangjie.analysis.test.framework.test.configurators

/**
 * 测试模块种类（对齐 Kotlin 的 TestModuleKind）。
 *
 * 决定测试中默认使用的 [CaModule][org.cangjie.analysis.api.CaModule] 类型。
 */
enum class TestModuleKind(val suffix: String) {
    Source("Source"),
    LibraryBinary("LibraryBinary"),
    LibrarySource("LibrarySource"),
    ScriptSource("ScriptSource"),
}
