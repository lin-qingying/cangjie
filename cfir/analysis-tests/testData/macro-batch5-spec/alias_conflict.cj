// MACRO_EXECUTOR: stub
// MACRO_DEFINITION: macros.foo.A,source=MACRO_ARTIFACT,expand=42
// MACRO_DEFINITION: macros.baz.B,source=MACRO_ARTIFACT,expand=43
// EXPECT_DEGRADED

// PLAN.md §12 Batch 5：alias 冲突独立用例。
//
// 同短名 `Bar` 被两条 import 绑到不同 fqn，触发 MacroResolutionContext
// 的 alias-conflict 检测，FrontendMacroConstructionService 上报
// MACRO_ALIAS_CONFLICT 诊断到原 import 位点（baseline §4 / §12 Batch 5）。
//
// 期望诊断由 ordinary checker collector 通过 macroExpansionRegistry 反查到
// import 处提交；DEGRADED 模式 construction 完成后 ordinary resolve 仍运行，
// 诊断流回 CjDiagnostic 流以匹配 inline marker。

package macro.alias_conflict_test

<!MACRO_ALIAS_CONFLICT!>import macros.foo.A as Bar<!>
<!MACRO_ALIAS_CONFLICT!>import macros.baz.B as Bar<!>

func useMacro(): Int {
    return @Bar()
}
