// 接口多继承与类多实现：interface <: A & B、class <: I1 & I2 与泛型接口。
// 官方依据：Cangjie 文档《接口》《继承》；cjc 1.0.5 / 1.1.3 零诊断。
interface Named {
    func name(): String
}

interface Sized {
    func size(): Int64
}

interface Multi <: Named & Sized {
    func describe(): String {
        return "multi"
    }
}

interface GenericBox<T> {
    func box(): T
}

class Impl <: Named & Sized {
    public init() {}

    public func name(): String {
        return "impl"
    }

    public func size(): Int64 {
        return 1
    }
}

class BoxImpl <: GenericBox<Int64> & Named {
    private var inner: Int64 = 0

    public init() {}

    public func box(): Int64 {
        return inner
    }

    public func name(): String {
        return "box"
    }
}
