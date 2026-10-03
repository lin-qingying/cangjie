// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：非泛型 extend 上写 where（官方 where 只允许泛型声明）。
// 官方 cjc 1.0.5 / 1.1.3：parse_unexpected_where "unexpected 'where' in non-generic declaration"。
struct Plain {
    var x: Int64 = 0
}

extend Plain where Plain <: Any {
    public func extra(): Int64 {
        return 1
    }
}
