// IMPORT_PATH: ../../libraries/digest_default_impl
// 接口给出 `finish()` 默认实现、实现类型只写 `finish(to!: Int64)`：
// 变量上调用 `sha256.finish()` 必须落到接口默认实现，而不是"函数值调用参数个数不符"。
import digestdefault.*

func useDefaultFinish(): Array<Byte> {
    var sha256 = SHA256()
    sha256.write([1])
    var digest: Array<Byte> = sha256.finish()
    sha256.reset()
    return digest
}

func useNamedFinish(): Array<Byte> {
    var sha256 = SHA256()
    sha256.write([1])
    var digest: Array<Byte> = sha256.finish(to: 16)
    sha256.reset()
    return digest
}
