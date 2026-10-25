// 函数声明：参数（含命名参数默认值）、返回类型、函数体与无体声明。
// 官方依据：Cangjie 文档《函数定义》；cjc 1.0.5 / 1.1.3 零诊断。
func simple(): Unit {}

func withParams(a: Int64, b: String): Int64 {
    return a
}

func withDefault(a!: Int64 = 1, b!: String = "x"): Int64 {
    return a
}

func withNamed(first: Int64, second!: Int64 = 0): Int64 {
    return first + second
}

func generic<T>(value: T): T {
    return value
}

func noReturnType(a: Int64) {
    return
}

interface Provider {
    func provide(): Int64
}

class Impl <: Provider {
    public func provide(): Int64 {
        return 1
    }
}
