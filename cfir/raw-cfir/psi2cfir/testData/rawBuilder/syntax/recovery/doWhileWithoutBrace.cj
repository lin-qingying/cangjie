// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：do-while 的循环体不带花括号（官方要求 `do { } while (...)`）。
// 官方 cjc 1.0.5 / 1.1.3：parse_expected_left_brace "expected '{', found 'i'"。
func doWhileWithoutBrace(): Int64 {
    var i = 0
    do i++ while (i < 3)
    return i
}
