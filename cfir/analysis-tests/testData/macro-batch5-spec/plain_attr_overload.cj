// MACRO_EXECUTOR: stub
// MACRO_DEFINITION: macros.attr.Attr,source=MACRO_ARTIFACT,supportsPlainAttrOverload=true,expand=
// MACRO_DEFINITION: macros.plain.PlainOnly,source=MACRO_ARTIFACT,supportsPlainAttrOverload=false,expand=
// EXPECT_DEGRADED

// PLAN.md §12 Batch 5：plain-attr overload 独立用例。
//
// `Attr` 的 MacroDefinitionEntry.supportsPlainAttrOverload = true，
// 允许在源码中既写 `@Attr` 也写 `@Attr(arg)`，两种形态都应解析成功。
// `PlainOnly` 的 supportsPlainAttrOverload = false，
// 只允许带括号 `@PlainOnly(...)`；无括号 `@PlainOnly` 调用方
// 期望 MACRO_UNRESOLVED 诊断（PLAIN_ATTR_OVERLOAD_NOT_SUPPORTED）。

package macro.plain_attr_overload_test

import macros.attr.Attr
import macros.plain.PlainOnly

@Attr
func okBare(): Int { return 1 }

@Attr(extra)
func okWithArgs(): Int { return 2 }

<!MACRO_UNRESOLVED!>@PlainOnly<!>
func notAllowed(): Int { return 3 }
