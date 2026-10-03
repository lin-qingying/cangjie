// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：下标 set 重载的第二个形参不是 `value!`（官方 set 形态固定为 `operator func [](index1: T1, ..., value!: TN)`；按名赋值时只认 `value`）。
// 官方 cjc 1.0.5 / 1.1.3：sema_unknown_named_argument "unknown named argument prefix 'value:'"。
class UnnamedValue {
    public init() {}

    public operator func [](i: Int64, v: Int64): Unit {
    }
}

func subscriptSetUnnamedParameter(m: UnnamedValue): Unit {
    m[0] = 1
}
