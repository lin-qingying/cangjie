package digestiface

// 与 SDK `std.crypto.digest.Digest` 同形：零参 `finish()` + 命名参数重载 `finish(to!: Int64)`。
public interface Digest {
    func write(data: Array<Byte>): Unit

    func finish(): Array<Byte>

    func finish(to!: Int64): Array<Byte>

    func reset(): Unit
}
