package org.cangnova.cangjie.analysis.test.framework.test.configurators

/**
 * 测试模块种类。
 *
 * 这里对齐 Kotlin Analysis API 测试框架的模块分类，但命名沿用仓颉侧的 `Ca*` 体系。
 * 模块种类会直接影响测试项目结构工厂生成的 `CaModule` 图，而不是仅作为输出后缀。
 */
enum class TestModuleKind(val suffix: String) {
    Source("Source"),
    LibraryBinary("LibraryBinary"),
    LibrarySource("LibrarySource"),
    ScriptSource("ScriptSource"),
    CodeFragment("CodeFragment"),
    NotUnderContentRoot("NotUnderContentRoot"),
}
