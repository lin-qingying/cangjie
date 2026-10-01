// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：match 的数组模式 `[1, ..rest]`（官方 parse_expected_pattern "expected pattern, found '['"；仓颉的 match 模式里没有数组模式，
// 前缀区间 `..` 也不是合法模式开头）。
// 官方 cjc 1.0.5 / 1.1.3：parse_expected_pattern。
func arrayPattern(xs: Array<Int64>): Int64 {
    return match (xs) {
        case [1, ..rest] => rest.size
        case _ => 0
    }
}
