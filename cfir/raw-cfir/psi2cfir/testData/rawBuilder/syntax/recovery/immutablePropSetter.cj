// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：非 mut 的 prop 写 setter（官方 sema_immutable_property_with_setter "immutable property cannot have setter"，锚 prop 声明）。
// 官方 cjc 1.0.5 / 1.1.3：sema_immutable_property_with_setter。
class C {
    private var v: Int64 = 0

    public prop p: Int64 {
        get() { return v }
        set(x) { v = x }
    }

    public init() {}
}
