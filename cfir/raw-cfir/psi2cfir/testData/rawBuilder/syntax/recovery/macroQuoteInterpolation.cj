// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：quote 内用字符串插值写法 `${x}`（官方 quote 的插值语法是 `$(x)`，1.0.5 / 1.1.3 都在解析阶段拒绝 `${`）。
// 官方 cjc 1.0.5 / 1.1.3：parse_expect_escape_dollar_token "expected identifier or '(' after '$'"。
// 附注：1.1.3 起 `$(x)` 展开语义改为调用 transformTokens，`$(x)` 在 1.1.3 会报 sema_undeclared_identifier 'transformTokens'；
//   1.0.5 / 1.1.3 都接受的花括号块体写法 `quote({ $(x) })` 见 macroDeclVariants.cj。
macro package macroquote
import std.ast.*

public macro withBraceInterpolation(input: Tokens): Tokens {
    return quote(${input})
}
