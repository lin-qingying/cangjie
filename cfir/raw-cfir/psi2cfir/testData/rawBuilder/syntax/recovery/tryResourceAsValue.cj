// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：把带资源的 try 当作表达式值（`let a = try (r = Res()) { 1 }`）——官方 SynTryWithResourcesExpr 把带资源 try 的类型固定为 Unit，
// 赋给 Int64 报 mismatched types。
// 官方 cjc 1.0.5 / 1.1.3：sema_mismatched_types。
class Res <: Resource {
    public init() {}

    public func isClosed(): Bool { return false }

    public func close(): Unit {}
}

class MyError <: Exception {
    public init() { super() }
}
func tryResourceAsValue(): Int64 {
    let a = try (res = Res()) {
        1
    }
    return a
}
