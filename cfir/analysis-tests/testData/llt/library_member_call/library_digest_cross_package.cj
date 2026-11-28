// IMPORT_PATH: ../../libraries/digest_cross_pkg
// 跨包形态（照 stdx）：接口 `Digest` 在 `digestiface`，实现 struct `SHA256` 在 `digestimpl`。
// 变量上调用 `sha256.finish()` 必须选零参重载，不能退化成参数个数不符。
import digestimpl.*

func useCrossPackageDigest(): Array<Byte> {
    var sha256 = SHA256()
    sha256.write([1])
    var digestStage1: Array<Byte> = sha256.finish()
    sha256.reset()
    sha256.write(digestStage1)
    var digestStage2: Array<Byte> = sha256.finish()
    sha256.reset()
    return digestStage2
}

func useCrossPackageDigestWithLength(): Array<Byte> {
    var sha256 = SHA256()
    sha256.write([1])
    var truncated: Array<Byte> = sha256.finish(to: 16)
    sha256.reset()
    return truncated
}
