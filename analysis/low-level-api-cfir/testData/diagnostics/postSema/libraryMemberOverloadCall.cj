// MODULE: lib
// FILE: lib.cj
package libpkg

// 库模块形态：接口同时声明零参 `finish()` 与命名参数重载 `finish(to!: Int64)`。
// 该 `.cjo` 的包头会依赖 `std.core`（`Array`），依赖解析必须能看到 SDK stdlib 根。
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

// MODULE: main(lib)
// FILE: libraryMemberOverloadCall.cj
package test

import libpkg.*

func useLibraryOverload(): Array<Byte> {
    var sha256 = SHA256()
    sha256.write([1])
    var digest: Array<Byte> = sha256.finish()
    sha256.reset()
    return digest
}

func useLibraryNamedOverload(): Array<Byte> {
    var sha256 = SHA256()
    sha256.write([1])
    var digest: Array<Byte> = sha256.finish(to: 16)
    sha256.reset()
    return digest
}
