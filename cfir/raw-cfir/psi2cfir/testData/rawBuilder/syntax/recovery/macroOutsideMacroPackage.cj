// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：普通包（非宏包）里声明宏。
// 官方 cjc 1.0.5 / 1.1.3：parse_expected_macro_decl_define_in_macro_package "macro declaration must be defined in macro package"。
import std.ast.*

public macro misplaced(input: Tokens): Tokens {
    return input
}
