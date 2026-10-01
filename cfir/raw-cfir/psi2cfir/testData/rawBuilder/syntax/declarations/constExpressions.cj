// RUN_PIPELINE_TILL: FRONTEND
// const 声明的初值形态：算术 / 字符串拼接 / 布尔运算 / 浮点运算 / 常量引用（const 表达式由编译器常量求值器确认）。
// 注意：数组字面量不是 const 表达式（官方 sema_expect_const，见 recovery/constArrayInitializer.cj）。
// 官方依据：Cangjie 文档《常量》；官方 CHIR/Analysis/ConstAnalysis.h；cjc 1.0.5 / 1.1.3 零诊断。
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
