// 数值边界：十六进制浮点无整数部分、Int64 最大值、UInt64 最大值与后缀下划线。
// 官方依据：Cangjie 文档《整数类型》《浮点类型》；cjc 1.0.5 / 1.1.3 零诊断。
func numericEdges(): Unit {
    let a = 0x1.8p3
    let b = 0x1p-1
    let c = 0x.8p1
    let d = 9223372036854775807i64
    let e = 18446744073709551615u64
    let f = 0xFFFF_FFFFu32
    let _ = (a, b, c, d, e, f)
}
