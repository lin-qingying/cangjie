// RUN_PIPELINE_TILL: FRONTEND
// try 资源（`try (r = expr) { }`）：资源类型必须实现 `Resource`（`isClosed()` + `close()`，官方 Sema/TypeCheckExpr/TryExpr.cpp SynTryWithResourcesExpr 用 core 的
// `Resource` 接口做子类型检查）；语句体、return、多个资源、显式类型、catch、finally 组合。带资源的 try 表达式类型是 Unit（不能当值用，见 recovery）。
// 官方依据：Cangjie 文档《异常处理》「资源管理」；官方 Sema/Desugar/AfterTypeCheck/TryExpr.cpp（展开为 `if (!r.isClosed()) { r.close() }`）；cjc 1.0.5 / 1.1.3 零诊断。
class Res <: Resource {
    public init() {}

    public func isClosed(): Bool { return false }

    public func close(): Unit {}
}

class MyError <: Exception {
    public init() { super() }
}
func tryResourceForms(): Unit {
    try (a = Res()) {
        let x = 1
        let _ = x
    }
    try (b = Res(), c = Res()) {
    }
    try (d: Res = Res()) {
    }
    try (e = Res()) {
    } catch (err: MyError) {
    }
    try (f = Res()) {
    } finally {
    }
}

func tryResourceWithReturn(): Int64 {
    try (g = Res()) {
        return 1
    }
    return 0
}
