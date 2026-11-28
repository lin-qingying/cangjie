// RUN_PIPELINE_TILL: FRONTEND
// 浮点字面量：十进制（有点 / 无点但有指数 / 前导点）、十六进制浮点（p 指数）、指数 E、大小写后缀、负号。
// 官方依据：Cangjie 文档《浮点类型》「浮点类型字面量」；cjc 1.0.5 / 1.1.3 零诊断。
func floatLiterals(): Unit {
    let plain = 3.14
    let expOnly = 2e3
    let expNegative = 2.4e-1
    let leadingDot = .123e2
    let upperExp = 1.5E3
    let hexFloat = 0x1.1p0
    let hexFloatNoDot = 0x1p2
    let hexFloatLeadingDot = 0x.2p4
    let hexUpper = 0X1.1P0
    let sfxF16 = 1.0f16
    let sfxF32 = 3.14f32
    let sfxF64 = 2e3f64
    let neg = -0.5
    let _ = (plain, expOnly, expNegative, leadingDot, upperExp, hexFloat, hexFloatNoDot, hexFloatLeadingDot)
    let _ = (hexUpper, sfxF16, sfxF32, sfxF64, neg)
}
