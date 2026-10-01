// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：match 的 case 写成 `case var v: T`（官方是 `case v: T`，没有 var 关键字）。
// 官方 cjc 1.0.5 / 1.1.3：parse_expected_pattern "expected pattern, found keyword 'var'"。
func varPatternInMatch(x: Any): Int64 {
    return match (x) {
        case var v: Int64 => v
    }
}
