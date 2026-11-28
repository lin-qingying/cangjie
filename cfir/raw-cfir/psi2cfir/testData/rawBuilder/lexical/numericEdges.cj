// RUN_PIPELINE_TILL: FRONTEND
// 数值字面量边界：十六进制浮点带 / 不带符号的指数（0x1.8p3、0x1p-1、0x.8p1）、整数字面量最大值与后缀组合（i64 / u64 / u32）。
// 官方依据：Cangjie 文档《数字字面量》；官方 Lexer.cpp 扫描数字（进制 / 下划线分隔 / 后缀 / 十六进制浮点）；cjc 1.0.5 / 1.1.3 零诊断。
func numericEdges(): Unit {
    let a = 0x1.8p3
    let b = 0x1p-1
    let c = 0x.8p1
    let d = 9223372036854775807i64
    let e = 18446744073709551615u64
    let f = 0xFFFF_FFFFu32
    let _ = (a, b, c, d, e, f)
}
