// RUN_PIPELINE_TILL: FRONTEND
// lambda 形态补充：花括号块体嵌套（`{ a: Int64 => { b: Int64 => a + b } }`）、返回值直接调用 `g(1)(2)`、尾随闭包带括号与花括号块体两种形态。
// 官方依据：Cangjie 文档《函数表达式》《函数调用语法糖》；cjc 1.0.5 / 1.1.3 零诊断。
func g2(x: Int64, f: (Int64) -> Int64): Int64 {
    return f(x)
}

func lambdaCallForms(): Int64 {
    let nested = { a: Int64 => { b: Int64 => a + b } }
    let called = nested(1)(2)
    let trailing = g2(1) { x => x + 1 }
    let trailingBlock = g2(1) { x: Int64 =>
        x + 1
    }
    return called + trailing + trailingBlock
}
