// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：宏声明缺少 `public` 修饰。
// 官方 cjc 1.0.5 / 1.1.3：parse_expected_public_before_macro_decl "macro declaration must be modified with 'public'"。
macro package macropublic
import std.ast.*

macro hidden(input: Tokens): Tokens {
    return input
}
