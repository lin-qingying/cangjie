package digestoverload

// 与 SDK `std.crypto.digest.Digest` 同形：接口同时声明零参 `finish()` 与命名参数重载
// `finish(to!: Int64)`，由 struct 分别实现。用于复现库侧（.cjo）同名重载成员调用。
public interface Digest {
    func write(data: Array<Byte>): Unit

    func finish(): Array<Byte>

    func finish(to!: Int64): Array<Byte>

    func reset(): Unit
}

public struct SHA256 <: Digest {
    public init() {
    }

    public func write(data: Array<Byte>): Unit {
        let _ = data
    }

    public func finish(): Array<Byte> {
        return Array<Byte>(32, repeat: 0)
    }

    public func finish(to!: Int64): Array<Byte> {
        return Array<Byte>(to, repeat: 0)
    }

    public func reset(): Unit {
    }
}
