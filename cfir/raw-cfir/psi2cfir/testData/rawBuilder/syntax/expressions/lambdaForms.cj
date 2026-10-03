// RUN_PIPELINE_TILL: FRONTEND
// lambda：无参 / 单参 / 多参 / 带类型标注的形参 / 块体 / 表达式体 / 尾随闭包（括号后与换行两种）/ 作为实参 / 赋给具函数类型的变量 /
// 返回函数（返回值的类型本身是函数；嵌套 lambda 用花括号块体）。
// 官方依据：Cangjie 文档《函数表达式》《函数调用语法糖》；cjc 1.0.5 / 1.1.3 零诊断（形参没有类型上下文时必须有标注；同一层只能有一个带标注的 lambda）。
func apply(f: (Int64, Int64) -> Int64): Int64 {
    return f(1, 2)
}

func applyOne(f: (Int64) -> Int64): Int64 {
    return f(1)
}

func lambdaForms(): Unit {
    let noArgs = { => 1 }
    let exprBody = { a: Int64 => a + 1 }
    let blockBody = { a: Int64 =>
        let b = a * 2
        b + 1
    }
    let typed: (Int64, Int64) -> Int64 = { a, b => a + b }
    let empty: () -> Unit = { => () }
    let single: (Int64) -> Int64 = { a => a }
    let asArg = apply { a, b => a * b }
    let asArgTyped = applyOne { a: Int64 => a - 1 }
    let trailing = applyOne { a => a + 100 }
    let trailingBlock = applyOne {
        a: Int64 =>
            a + 200
    }
    let _ = (noArgs(), exprBody(1), blockBody(1), typed(1, 2), empty(), single(1), asArg, asArgTyped, trailing, trailingBlock)
}

func returnsLambda(): (Int64) -> (String) -> Int64 {
    return { a: Int64 => { b: String => a } }
}
