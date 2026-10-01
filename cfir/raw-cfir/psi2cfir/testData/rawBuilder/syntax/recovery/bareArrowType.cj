// RUN_PIPELINE_TILL: FRONTEND
// 函数类型不能省略参数列表的括号（官方 parse_expected_parentheses / parse_expected_character "should be surrounded by '(' and ')'"），
// 包括 `Int64 -> Unit`、`?Int64 -> Unit`、`Array<Int64> -> Unit` 三种。
// 官方依据：官方 ParseType.cpp ParseType / DiagParseExpectedParenthis；cjc 1.0.5 / 1.1.3：parse_expected_parentheses / parse_expected_character。
type A = Int64 -> Unit

type B = ?Int64 -> Unit

type C = Array<Int64> -> Unit
