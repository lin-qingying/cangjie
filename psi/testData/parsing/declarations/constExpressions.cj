// 常量表达式：顶层 const 的算术、字符串拼接、逻辑与引用组合。
// 官方依据：Cangjie 文档《常量》；cjc 1.0.5 / 1.1.3 零诊断。
const A: Int64 = 1 + 2
const B: Int64 = 3 * 4
const C: String = "s" + "t"
const D: Bool = true && false
const E: Float64 = 1.0 * 2.0
const F: Int64 = 7
const G: Int64 = A * F

func constExpressions(): Int64 {
    return A + B + F + G
}
