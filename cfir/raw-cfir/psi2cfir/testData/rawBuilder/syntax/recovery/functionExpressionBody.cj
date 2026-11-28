// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：函数用 `= expr` 表达式体（官方 parse_missing_body "body of function 'f' is missing"，锚 `=` 后位置）。
// 官方 cjc 1.0.5 / 1.1.3：parse_missing_body / parse_expected_character。
func f(): Int64 = 1

func g(x: Int64) = x
