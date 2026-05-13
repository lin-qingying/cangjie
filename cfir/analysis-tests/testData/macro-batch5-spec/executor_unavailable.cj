// MACRO_EXECUTOR: none
// MACRO_DEFINITION: macros.external.Reformat,source=MACRO_ARTIFACT
// EXPECT_DEGRADED

// PLAN.md §12 Batch 5：executor ABI / 不可用独立用例。
//
// 测试 directive `MACRO_EXECUTOR=none` 表示 CompilerConfiguration 不注入
// macroExecutorFactory。FrontendMacroConstructionService.evaluateExternalMacro
// 在 executor == null 或 !isAvailable 时报告
// MACRO_EXECUTOR_UNAVAILABLE 诊断到 macro call 位点。
//
// DEGRADED 模式下 ordinary checker 仍运行，construction 诊断被
// MacroConstructionDiagnosticCollectorComponent 流回 CjDiagnostic 流。

package macro.executor_unavailable_test

import macros.external.Reformat

func useMacro(): Int {
    return <!MACRO_EXECUTOR_UNAVAILABLE!>@Reformat(1 + 2)<!>
}
