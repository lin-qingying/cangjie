// 形态照 mariadb-driver `src/connection/plugin.cj`：直接使用 SDK 库包 `std.crypto.digest`。
// 该接口同时声明 `finish()` 与命名参数重载 `finish(to!: Int64)`；调用 `digest.finish()`
// 必须选零参重载。CFIR 若把 `finish` 当单参函数值再调用，会报
// "Wrong number of arguments for function value call."。
//
// 注：`digest.finish(to: 16)` 目前仍报 `ARGUMENT_TYPE_MISMATCH`（库侧命名参数的类型解析），
// 与 mariadb 的 E3 无关，已记录为独立待查项，因此本用例只覆盖零参调用。
import std.crypto.digest.*

func digestViaInterface(digest: Digest): Array<Byte> {
    digest.write([1])
    var result: Array<Byte> = digest.finish()
    digest.reset()
    return result
}
