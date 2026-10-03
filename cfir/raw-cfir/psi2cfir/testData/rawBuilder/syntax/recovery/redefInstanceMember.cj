// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：对实例函数 / 实例 prop 用 `redef`（官方 redef 只能修饰 static 成员；实例成员用 override）。
// 官方 cjc 1.0.5 / 1.1.3：sema_redef_modify_static_func "'redef' cannot be used to modify an instance 'function'" /
// sema_func_no_override_or_redefine_modifier。
open class RedefBase {
    public init() {}

    public func f(): Int64 {
        return 1
    }

    public prop p: Int64 {
        get() { return 1 }
    }
}

class RedefDerived <: RedefBase {
    public init() {
        super()
    }

    public redef func f(): Int64 {
        return 2
    }

    public redef prop p: Int64 {
        get() { return 2 }
    }
}
