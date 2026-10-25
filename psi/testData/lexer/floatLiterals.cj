// 浮点数字面量：小数点形式、科学计数法、十六进制浮点与后缀。
// 官方依据：Cangjie 文档《浮点类型》；cjc 1.0.5 / 1.1.3 零诊断。
func floatLiterals(): Unit {
    let point = 3.14
    let leadingPoint = .5
    let trailingPoint = 5.0
    let exponent = 1e10
    let exponentSigned = 2.5E-3
    let exponentPlus = 2.5e3
    let withUnderscores = 1_000.000_1
    let hexFloat = 0x1.8p3
    let hexFloatLower = 0x1.8p-2
    let suffixF16 = 1.0f16
    let suffixF32 = 1.0f32
    let suffixF64 = 1.0f64
    let _ = (point, leadingPoint, trailingPoint, exponent, exponentSigned, exponentPlus)
    let _ = (withUnderscores, hexFloat, hexFloatLower)
    let _ = (suffixF16, suffixF32, suffixF64)
}
