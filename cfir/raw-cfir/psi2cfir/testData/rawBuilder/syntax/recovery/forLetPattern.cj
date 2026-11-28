// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：for 循环里用 let 模式 `for (let v <- xs)`（官方 parse_expected_pattern "expected pattern, found keyword 'let'"）。
// 官方 cjc 1.0.5 / 1.1.3：parse_expected_pattern。
func forLetPattern(): Int64 {
    var t = 0
    for (let v <- [1, 2]) {
        t += v
    }
    return t
}
