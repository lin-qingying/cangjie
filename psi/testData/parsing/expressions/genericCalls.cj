// 泛型调用：显式类型参数、泛型构造调用与泛型方法链。
// 官方依据：Cangjie 文档《泛型》；cjc 1.0.5 / 1.1.3 零诊断。
func id<T>(x: T): T {
    return x
}

func empty<T>(): T {
    throw Exception()
}

class Box<T> {
    var v: T

    public init(v: T) {
        this.v = v
    }

    public func get(): T {
        return v
    }
}

func genericCalls(): Int64 {
    let a = id<Int64>(1)
    let b = id<String>("x")
    let c = Box<Int64>(1)
    let d = c.get()
    let e = id<Box<Int64>>(c).get()
    return a + d + e
}
