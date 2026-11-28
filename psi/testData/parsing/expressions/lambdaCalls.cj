// lambda 调用：嵌套 lambda 逐级调用、尾随闭包（单行与块体）。
// 官方依据：Cangjie 文档《函数表达式》；cjc 1.0.5 / 1.1.3 零诊断。
func g2(x: Int64, f: (Int64) -> Int64): Int64 {
    return f(x)
}

func lambdaCalls(): Int64 {
    let nested = { a: Int64 => { b: Int64 => a + b } }
    let called = nested(1)(2)
    let trailing = g2(1) { x => x + 1 }
    let trailingBlock = g2(1) { x: Int64 =>
        x + 1
    }
    return called + trailing + trailingBlock
}
