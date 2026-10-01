// RUN_PIPELINE_TILL: FRONTEND
// 普通标识符：XID_Start / XID_Continue、下划线开头、结尾下划线、中文与日文、单独的下划线通配符。
// 官方依据：Cangjie 文档《标识符》；cjc 1.0.5 / 1.1.3 零诊断。
func ordinaryIdentifiers(): Unit {
    let abc = 1
    let _abc = 2
    let abc_ = 3
    let a1b2c3 = 4
    let a_b_c = 5
    let a1_b2_c3 = 6
    let 仓颉 = 7
    let __こんにちは = 8
    let _ = 9
    let _ = (abc, _abc, abc_, a1b2c3, a_b_c, a1_b2_c3, 仓颉, __こんにちは)
}
