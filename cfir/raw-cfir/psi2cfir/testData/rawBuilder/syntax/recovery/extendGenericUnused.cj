// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：extend 的类型形参没有出现在被扩展类型里（官方 sema_extend_generic_must_be_used "type parameter 'T' must be used in extended type"，锚 `T`）。
// 合法写法见 declarations/extendBuiltinForms.cj 的 `extend<T> G<T>`。
// 官方 cjc 1.0.5 / 1.1.3：sema_extend_generic_must_be_used。
struct P {
    public init() {}
}

extend<T> P {
    public func g(t: T): Int64 { return 0 }
}
