// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：在类外调用私有构造器（私有构造器只能在类内调用，见 declarations/memberInitForms.cj）。
// 官方 cjc 1.0.5 / 1.1.3：sema_no_match_constructor "no matching constructor for call 'C'"。
class C {
    var v: Int64 = 0

    private init(v: Int64) {
        this.v = v
    }
}

func privateInitCalledOutside(): C {
    return C(1)
}
