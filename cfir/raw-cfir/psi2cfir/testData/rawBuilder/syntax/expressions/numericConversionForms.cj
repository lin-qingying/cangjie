// RUN_PIPELINE_TILL: FRONTEND
// 数值与字面量转换：UInt8 -> Int64、Int64 -> Float64、Float64 -> Int64、Rune 字面量赋给 Rune、String / Bool 的平凡赋值。
// 官方依据：Cangjie 文档《类型转换》；cjc 1.0.5 / 1.1.3 零诊断。
func numericConversionForms(i: Int64, u: UInt8, fl: Float64, r: Rune, s: String, b: Bool): Unit {
    let a: Int64 = Int64(u)
    let b2: Float64 = 1.0
    let c: Int64 = Int64(fl)
    let d: Rune = r'x'
    let e: String = s
    let g: Bool = b
    let h: Int64 = i
    let _ = (a, b2, c, d, e, g, h)
}
