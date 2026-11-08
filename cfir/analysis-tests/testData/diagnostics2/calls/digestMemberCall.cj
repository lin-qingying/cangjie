// 形态照 `std.crypto.digest` 的 `Digest` 接口 + stdx `SHA256`（mariadb-driver `plugin.cj` 调用点）：
// 接口同时声明 `finish()` 与 `finish(to: Int64)` 两个同名重载，实现类型再分别实现它们。
// 变量上调用 `sha256.finish()` 必须选零参重载；CFIR 若把 `finish` 当单参函数值再调用，
// 会报 "Wrong number of arguments for function value call."。
interface Digest {
    func write(data: Array<Byte>): Unit

    func finish(): Array<Byte>

    func finish(to: Int64): Array<Byte>

    func reset(): Unit
}

struct SHA256 <: Digest {
    public init() {
    }

    public func write(data: Array<Byte>): Unit {
        let _ = data
    }

    public func finish(): Array<Byte> {
        return []
    }

    public func finish(to: Int64): Array<Byte> {
        let _ = to
        return []
    }

    public func reset(): Unit {
    }
}

func cachingSha2Password(password: Array<Byte>): Array<Byte> {
    var sha256 = SHA256()
    sha256.write(password)
    var digestStage1: Array<Byte> = sha256.finish()
    sha256.reset()
    sha256.write(digestStage1)
    var digestStage2: Array<Byte> = sha256.finish()
    sha256.reset()
    return digestStage2
}

func finishWithLength(): Array<Byte> {
    var sha256 = SHA256()
    sha256.write([1])
    var truncated: Array<Byte> = sha256.finish(16)
    sha256.reset()
    return truncated
}
