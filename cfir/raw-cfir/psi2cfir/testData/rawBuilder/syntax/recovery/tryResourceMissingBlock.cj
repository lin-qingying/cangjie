// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：try 资源后没有块体（`try (r = Res())` 直接换行）（官方 parse_expected_left_brace "expected '{', found '}'"，锚 `}`）。
// 官方 cjc 1.0.5 / 1.1.3：parse_expected_left_brace。
class Res <: Resource {
    public init() {}

    public func isClosed(): Bool { return false }

    public func close(): Unit {}
}

class MyError <: Exception {
    public init() { super() }
}
func tryResourceMissingBlock(): Unit {
    try (res = Res())
}
