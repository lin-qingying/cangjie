package digestdefault

// 与 SDK `std.crypto.digest.Digest` 假设同形：接口层面给出 `finish()` 的默认实现，
// 另声明命名参数重载 `finish(to!: Int64)`；实现类型只实现后者，零参版本走接口默认实现。
public interface Digest {
    func write(data: Array<Byte>): Unit

    func finish(): Array<Byte> {
        return Array<Byte>(0, repeat: 0)
    }

    func finish(to!: Int64): Array<Byte>

    func reset(): Unit
}

public struct SHA256 <: Digest {
    public init() {
    }

    public func write(data: Array<Byte>): Unit {
        let _ = data
    }

    public func finish(to!: Int64): Array<Byte> {
        return Array<Byte>(to, repeat: 0)
    }

    public func reset(): Unit {
    }
}
