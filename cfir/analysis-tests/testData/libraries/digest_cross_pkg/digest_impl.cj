package digestimpl

// 与 stdx `stdx.crypto.digest.SHA256` 同形：实现在另一个包里的接口。
public import digestiface.*

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
