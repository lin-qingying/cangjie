// IMPORT_PATH: ../../libraries/digest_overload
// 库侧（.cjo）接口同时有 `finish()` 与命名参数重载 `finish(to!: Int64)`：
// 变量上调用 `sha256.finish()` 必须选零参重载，不能退化成"函数值调用参数个数不符"。
import digestoverload.*

func useDigest(): Array<Byte> {
    var sha256 = SHA256()
    sha256.write([1])
    var digestStage1: Array<Byte> = sha256.finish()
    sha256.reset()
    sha256.write(digestStage1)
    var digestStage2: Array<Byte> = sha256.finish()
    sha256.reset()
    return digestStage2
}

func useDigestWithLength(): Array<Byte> {
    var sha256 = SHA256()
    sha256.write([1])
    var truncated: Array<Byte> = sha256.finish(to: 16)
    sha256.reset()
    return truncated
}
