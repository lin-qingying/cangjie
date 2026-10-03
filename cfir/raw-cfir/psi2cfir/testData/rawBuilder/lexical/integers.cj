// RUN_PIPELINE_TILL: FRONTEND
// 整数字面量：四种进制、下划线分隔、前缀大小写、类型后缀、负号。
// 官方依据：Cangjie 文档《整数类型》「整数类型字面量」；cjc 1.0.5 / 1.1.3 零诊断。
func integerLiterals(): Unit {
    let dec = 1_000_000
    let bin = 0b0001_1000
    let binUpper = 0B1111
    let oct = 0o30
    let octUpper = 0O17
    let hex = 0x18
    let hexUpper = 0X1F
    let sfxI8 = 100i8
    let sfxI16 = 1i16
    let sfxI32 = 1i32
    let sfxI64 = 1i64
    let sfxU8 = 1u8
    let sfxU16 = 1u16
    let sfxU32 = 1u32
    let sfxU64 = 1u64
    let neg = -42
    let negHex = -0x10
    let zero = 0
    let _ = (dec, bin, binUpper, oct, octUpper, hex, hexUpper, sfxI8, sfxI16, sfxI32, sfxI64)
    let _ = (sfxU8, sfxU16, sfxU32, sfxU64, neg, negHex, zero)
}
