// MACRO_EXECUTOR: stub
// MACRO_DEFINITION: macros.plain.PlainOnly,source=MACRO_ARTIFACT,supportsForcedKind=false,expand=42
// EXPECT_DEGRADED

// PLAN.md §12 Batch 5：`@!` 强制形式 (forced kind) 不匹配独立用例。
//
// 对一个 supportsForcedKind=false 的宏使用 `@!` 调用形式。
// MacroResolutionContext.resolveMacroCall 在 verifyCallShapeOrMismatch 内
// 返回 KindMismatch，FrontendMacroConstructionService.expandResolvedSurface
// 报告 MACRO_UNRESOLVED 诊断（消息含 "does not support `FORCED` invocation"）。

package macro.forced_kind_test

import macros.plain.PlainOnly

func useMacro(): Int {
    return <!MACRO_UNRESOLVED!>@!PlainOnly(99)<!>
}
