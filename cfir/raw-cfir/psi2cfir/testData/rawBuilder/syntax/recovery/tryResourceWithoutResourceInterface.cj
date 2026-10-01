// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：try 资源的类型没有实现 `Resource`（官方 Sema/TypeCheckExpr/TryExpr.cpp SynTryWithResourcesExpr：mismatched types）。
// 官方 cjc 1.0.5 / 1.1.3：sema_mismatched_types。
class NotResource {
    public init() {}

    public func isClosed(): Bool { return false }

    public func close(): Unit {}
}

func tryResourceWithoutResourceInterface(): Unit {
    try (res = NotResource()) {
    }
}
