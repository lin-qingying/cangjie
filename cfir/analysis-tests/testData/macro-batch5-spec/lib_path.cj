// MACRO_EXECUTOR: stub
// MACRO_DEFINITION: macros.artifact.WithLib,source=MACRO_ARTIFACT,libPath=stub.dylib,expand=42

// PLAN.md §12 Batch 5：lib path 流转独立用例。
//
// 引入声明在 macro artifact 中的宏 `WithLib`，对应的
// MacroDefinitionEntry.libPath = "stub.dylib"。
// FrontendMacroConstructionService.evaluateExternalMacro 在调用 executor 前会
// 触发 `executor.loadLibraries(listOf("stub.dylib"))`；
// 程序级 batch5_libPathFlowsThroughExecutorLoadLibraries 已断言 lib path 被传递。
//
// 本 testdata 验证 stub executor + 注入 macro definition 后无诊断：
// `@WithLib(42)` 通过 macro construction 成功展开为 Int 字面量。

package macro.lib_path_test

import macros.artifact.WithLib

func useMacro(): Int {
    return @WithLib(42)
}
