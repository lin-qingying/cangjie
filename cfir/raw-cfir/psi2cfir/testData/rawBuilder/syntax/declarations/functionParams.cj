// RUN_PIPELINE_TILL: FRONTEND
// 函数形参：空列表、无名形参、具名形参（x!）、具名形参默认值（只允许具名）、函数类型形参、形参中的函数类型名。
// 官方依据：Cangjie 文档《函数定义》「具名参数」；cjc 1.0.5 / 1.1.3 零诊断（无名形参后不能跟具名形参、默认值只在具名形参上）。
func noParams(): Unit {
}

func unnamed(a: Int64, b: String): Unit {
    let _ = (a, b)
}

func named(x!: Int64, y!: String): Unit {
    let _ = (x, y)
}

func namedWithDefault(x!: Int64 = 1, y!: String = "d"): Unit {
    let _ = (x, y)
}

func mixed(a: Int64, b!: String = "d", c!: Bool = true): Unit {
    let _ = (a, b, c)
}

func functionParam(f: (Int64) -> Int64, g!: (Int64, Int64) -> Unit): Unit {
    let _ = (f(1), g)
}

func returns(): (Int64) -> Int64 {
    return { a: Int64 => a + 1 }
}
