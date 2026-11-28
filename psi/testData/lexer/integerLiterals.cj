// 整数字面量：十进制、十六进制、二进制、八进制，支持下划线分隔与后缀。
// 官方依据：Cangjie 文档《整数类型》；cjc 1.0.5 / 1.1.3 零诊断。
func integerLiterals(): Unit {
    let zero = 0
    let decimal = 42
    let withUnderscores = 1_000_000
    let hex = 0xDEAD_BEEF
    let hexLower = 0xdeadbeef
    let binary = 0b1010_1100
    let octal = 0o755
    let suffixI8 = 42i8
    let suffixI16 = 42i16
    let suffixI32 = 42i32
    let suffixI64 = 42i64
    let suffixU8 = 42u8
    let suffixU16 = 42u16
    let suffixU32 = 42u32
    let suffixU64 = 42u64
    let _ = (zero, decimal, withUnderscores, hex, hexLower, binary, octal)
    let _ = (suffixI8, suffixI16, suffixI32, suffixI64)
    let _ = (suffixU8, suffixU16, suffixU32, suffixU64)
}
