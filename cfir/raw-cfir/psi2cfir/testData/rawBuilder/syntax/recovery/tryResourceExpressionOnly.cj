// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：try 资源只写表达式不写赋值（`try (Res()) { }`）（官方 parse_expected_assignment "expected '=', found '('"）。
// 官方 cjc 1.0.5 / 1.1.3：parse_expected_assignment。
class Res <: Resource {
    public init() {}

    public func isClosed(): Bool { return false }

    public func close(): Unit {}
}

func tryResourceExpressionOnly(): Unit {
    try (Res()) {
    }
}
