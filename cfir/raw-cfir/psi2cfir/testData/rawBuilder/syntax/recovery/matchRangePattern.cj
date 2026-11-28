// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：match 的 case 使用区间模式（官方没有区间模式）。
// 官方 cjc 1.0.5 / 1.1.3：parse_expected_double_arrow_in_case。
func matchRangePattern(x: Int64): Int64 {
    return match (x) {
        case 1..10 => 1
        case _ => 0
    }
}
