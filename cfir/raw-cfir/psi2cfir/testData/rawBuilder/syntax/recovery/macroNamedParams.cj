// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：宏声明使用具名形参或非 Tokens 形参（官方宏形参必须是 Tokens 且不能具名）。
// 官方 cjc 1.0.5 / 1.1.3：parse_macro_illegal_named_param "cannot use named parameter in macro declaration"、
//   parse_macro_illegal_param_type "macro declaration's parameter type must be 'Tokens'"。
macro package macronamed
import std.ast.*

public macro named(input!: Tokens): Tokens {
    return input
}

public macro typed(input: Int64): Tokens {
    return quote(${input})
}
