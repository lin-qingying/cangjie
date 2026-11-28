// 函数类型引用：无参 / 单参 / 多参 / 具名形参函数类型、返回函数与函数类型属性。
// 官方依据：Cangjie 文档《函数类型》；cjc 1.0.5 / 1.1.3 零诊断。
func noArgs(f: () -> Unit): Unit {
    f()
}

func oneArg(f: (Int64) -> Int64): Int64 {
    return f(1)
}

func twoArgs(f: (Int64, String) -> Bool): Bool {
    return f(1, "x")
}

func namedArgs(f: (x: Int64, y: String) -> Bool): Bool {
    return f(1, "x")
}

func returningFunction(): (Int64) -> (String) -> Int64 {
    return { a: Int64 => { b: String => a } }
}

class WithFuncProp {
    private var handler: (Int64) -> Int64 = { a => a }

    public init() {}

    mut prop handlerProp: (Int64) -> Int64 {
        get() {
            return handler
        }
        set(v) {
            handler = v
        }
    }

    public func apply(x: Int64): Int64 {
        return handler(x)
    }
}
