// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：quote 里用 `$ast` / `$(x)` 插值。1.0.5 报 undeclared identifier 'ast' / 'x'（quote 的 `$(x)` 在 1.0.5 展开语义由编译器处理，但作为表达式引用名未声明）；
// 1.1.3 起 `$(x)` 展开语义改为调用 `transformTokens`，报 undeclared identifier 'transformTokens'。
// 官方 cjc 1.0.5：sema_undeclared_identifier 'ast' / 'x'；1.1.3：sema_undeclared_identifier 'transformTokens'。
macro package mp
import std.ast.*

public macro dollarIdent(input: Tokens): Tokens {
    return quote($ast)
}

public macro dollarParen(input: Tokens): Tokens {
    return quote($(input))
}
