// 函数形参形态：无名、具名（name!）、带默认值与混合形参列表。
// 官方依据：Cangjie 文档《函数定义》；cjc 1.0.5 / 1.1.3 零诊断。
func noParams(): Unit {}

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

func initNamed(id: Int64, tag!: String = "t"): Unit {
    let _ = (id, tag)
}
