// 调用与构造表达式：位置实参、具名实参、默认参数、泛型实参推断、构造调用与尾随闭包。
// 官方依据：Cangjie 文档《函数调用》《构造》；cjc 1.0.5 / 1.1.3 零诊断。
class Config {
    public var host: String = "localhost"
    public var port: Int64 = 80

    public init(host!: String = "localhost", port!: Int64 = 80) {
        this.host = host
        this.port = port
    }
}

func repeat(times: Int64, body!: () -> Unit): Unit {
    var i = 0
    while (i < times) {
        body()
        i++
    }
}

func combine(a: Int64, b: Int64, c: Int64): Int64 {
    return a + b + c
}

func identity<T>(value: T): T {
    return value
}

func calls(): Int64 {
    let cfg = Config()
    let custom = Config(host: "example.com", port: 8080)
    let sum = combine(1, 2, 3)
    let named = combine(1, 2, 3)
    let mixed = combine(3, 2, 1)
    let generic = identity<Int64>(42)
    repeat(3) {
        let _ = cfg
        let _ = custom
    }
    return sum + named + mixed + generic + custom.port
}
