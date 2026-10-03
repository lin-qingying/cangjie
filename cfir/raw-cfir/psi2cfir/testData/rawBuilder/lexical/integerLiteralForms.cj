// RUN_PIPELINE_TILL: FRONTEND
// 整数字面量形态：十进制 / 二进制（0b、下划线分隔）/ 八进制（0o）/ 十六进制（0X）、下划线分隔、字面量后缀与上界。
// 官方依据：Cangjie 文档《数字字面量》；官方 Lexer.cpp 数字扫描（进制前缀 / 下划线 / 后缀）；cjc 1.0.5 / 1.1.3 零诊断。
func integerLiteralForms(): Unit {
    let dec = 42
    let decSep = 1_000_000
    let bin = 0b1010_1010
    let binUpper = 0B1111_1111
    let oct = 0o755
    let octUpper = 0O17
    let hex = 0xCAFE
    let hexUpper = 0XBEEF
    let hexSep = 0xFF_FF
    let zero = 0
    let maxI64 = 9223372036854775807i64
    let maxU64 = 18446744073709551615u64
    let minI8 = -128i8
    let maxI8 = 127i8
    let maxU8 = 255u8
    let _ = (dec, decSep, bin, binUpper, oct, octUpper, hex, hexUpper, hexSep)
    let _ = (zero, maxI64, maxU64, minI8, maxI8, maxU8)
}
