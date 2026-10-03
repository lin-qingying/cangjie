// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：for-in 的匹配值写成 `let` 模式（1.0.5 / 1.1.3 的 for-in 只接受变量名或通配符，可选值解构用 while/if let）。
// 官方 cjc 1.0.5 / 1.1.3：parse_expected_pattern "expected pattern, found keyword 'let'"。
func forWithLetPattern(o: ?Int64): Unit {
    for (let Some(x) <- o) {
        println(x)
    }
}
