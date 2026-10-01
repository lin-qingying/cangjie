// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：普通包（非 macro package）里用 quote 表达式 / 非宏函数里写宏体。1.0.5 允许解析但报 sema_use_expr_without_import "import 'std.ast' to use the 'quote' expression"（未 import）与
// undeclared identifier 'refreshTokensPosition'；1.1.3 报 undeclared identifier 'transformTokens'。合法写法见 declarations/macroDeclVariants.cj（macro package + import std.ast.*）。
// 官方 cjc 1.0.5 / 1.1.3：sema_use_expr_without_import / sema_undeclared_identifier（版本差异见文件头）。
import std.ast.*

func quoteOutsideMacroPackage(): Unit {
    let a = quote(1)
    let _ = a
}
