// extend：无约束扩展、带上界扩展、泛型扩展与接口实现扩展。
// 官方依据：Cangjie 文档《extend》；cjc 1.0.5 / 1.1.3 零诊断。
interface Display {
    func show(): String
}

class Host {
    public init() {}
}

extend Host <: Display {
    public func show(): String {
        return "host"
    }
}

extend<T> Wrapper<T> <: Display where T <: Display {
    public func show(): String {
        return "wrapper"
    }
}

class Wrapper<T> {
    private var inner: T

    public init(inner: T) {
        this.inner = inner
    }
}

extend Int64 {
    func doubled(): Int64 {
        return this * 2
    }
}
