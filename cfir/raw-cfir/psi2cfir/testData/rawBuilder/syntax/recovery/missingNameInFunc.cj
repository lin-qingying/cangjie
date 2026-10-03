// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：函数名缺失。
// 官方 cjc 1.0.5 / 1.1.3：parse_expected_name "expected a func name after keyword 'func', found '('"。
func (a: Int64): Int64 {
    return a
}
