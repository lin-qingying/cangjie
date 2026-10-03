// RUN_PIPELINE_TILL: FRONTEND
// 证据：官方 cjc 1.0.5 实测：`swap(inout b.value, 1)`（`b` 是 class 实例）→
// error: the variable qualified by 'inout' cannot be directly or indirectly derived from an instance of a 'class'（锚 receiver `b`）。
// CFIR CfirInoutSemanticsChecker.checkInoutTarget 对 class receiver 报 INOUT_MODIFY_HEAP_VARIABLE（锚 receiver `b`）；
// 同一写法作用在 struct receiver 上时，CFIR 走 INOUT_MUST_BE_VAR_VARIABLE 分支（见下方对照用例）。
foreign func swap(value: Int64, other: Int64): Unit

class Box {
    var value: Int64 = 0
}

struct SBox {
    var value: Int64 = 0
}

func classField(b: Box): Unit {
    unsafe {
        swap(inout <!INOUT_MODIFY_HEAP_VARIABLE!>b<!>.value, 1)
    }
}

func structField(s: SBox): Unit {
    unsafe {
        swap(inout <!INOUT_MUST_BE_VAR_VARIABLE!>s<!>.value, 1)
    }
}
