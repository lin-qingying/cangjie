// RUN_PIPELINE_TILL: FRONTEND
struct Counter {
    var value: Int64 = 0

    mut func inc(): Unit {
        value = value + 1
    }

    func readOnlyAssign(): Unit {
        <!CANNOT_MODIFY_VAR!>value<!> = 1
    }

    func readOnlyCall(): Unit {
        <!IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION!>inc<!>()
    }

    mut func allowed(): Unit {
        value = value + 1
        inc()
    }
}

// P3 批次 2 回归：let 绑定的 struct 值调用 mut 成员函数（官方 sema_immutable_access_mutable_func）。
struct S {
    var value: Int64 = 0
    mut func set(v: Int64): Unit {
        value = v
    }
}

func letReceiverSet(): Unit {
    let s = S()
    <!IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION!>s<!>.set(1)
}
