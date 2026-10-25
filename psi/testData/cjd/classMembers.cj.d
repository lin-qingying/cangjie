// 无体声明（二）：class / struct / enum / extend 成员的无体形式。
// 官方依据：Cangjie 文档《类》《struct》《枚举》《extend》；docs/cjd-declaration-file-support-v3.md。
class Service {
    public func start(): Unit
    public func stop(): Unit
    public prop running: Bool
    private var state: Int64

    public init(state: Int64)
}

struct Config {
    public let host: String
    public let port: Int64

    public init(host: String, port: Int64)
    public func address(): String
}

enum State {
    Idle | Running | Stopped

    public func isTerminal(): Bool
}

extend Config {
    public func url(): String
}
