// 无体声明（五）：extend 的约束形式与枚举成员函数。
// 官方依据：Cangjie 文档《extend》《枚举》；docs/cjd-declaration-file-support-v3.md。
interface Display {
    func show(): String
}

extend<T> Wrapper<T> <: Display where T <: Display {
    func show(): String
}

enum State {
    | Idle
    | Running

    public func isTerminal(): Bool
}

extend Int64 {
    func doubled(): Int64
}
