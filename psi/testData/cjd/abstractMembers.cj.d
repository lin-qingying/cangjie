// 无体声明（一）：抽象类与接口的无体成员——仅 .cj.d 声明文件合法的形态。
// 官方依据：Cangjie 文档《抽象类》《接口》；docs/cjd-declaration-file-support-v3.md。
abstract class Shape {
    public func area(): Int64
    public func describe(): String
}

abstract class Named<T> {
    public func name(): T
    public func rename(value: T): Unit
}

interface Display {
    func show(): String
    prop label: String
    operator func ==(other: Display): Bool
}

interface Handler {
    func handle(event: String): Int64
}
