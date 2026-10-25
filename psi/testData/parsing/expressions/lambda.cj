// lambda 表达式：无参、单参、多参、带类型标注形参、块体与表达式体、尾随闭包。
// 官方依据：Cangjie 文档《函数表达式》；cjc 1.0.5 / 1.1.3 零诊断。
func apply(f: (Int64, Int64) -> Int64): Int64 {
    return f(1, 2)
}

func applyOne(f: (Int64) -> Int64): Int64 {
    return f(1)
}

func runUnit(body: () -> Unit): Unit {
    body()
}

func lambdas(): Int64 {
    let noArgs = { => 1 }
    let exprBody = { a: Int64 => a + 1 }
    let blockBody = { a: Int64 =>
        let b = a * 2
        b + 1
    }
    let typed: (Int64, Int64) -> Int64 = { a, b => a + b }
    let empty: () -> Unit = { => () }
    let single: (Int64) -> Int64 = { a => a }
    runUnit { =>
        let _ = noArgs
    }
    return apply(typed) + applyOne(single) + exprBody(1) + blockBody(1) + noArgs()
}
