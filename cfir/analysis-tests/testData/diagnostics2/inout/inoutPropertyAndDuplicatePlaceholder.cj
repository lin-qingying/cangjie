// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
foreign func swap(value: Int64, other: Int64): Unit

struct Box {
    var value: Int64 = 0
}

func propertyInout(box: Box): Unit {
    <!INVALID_INOUT_ARGUMENT!>/* suggested message: inout argument must be a mutable l-value and property passing rules are not yet fully modeled here. source: manual_source_zh_cn_cangjie_c_inout_参数_3; docs/diagnostics-gap-vs-official-cpp-sema-detailed.md */swap(inout box.value, 1)<!>
}

func duplicateInout(): Unit {
    var x = 1
    <!DUPLICATE_INOUT_ARGUMENT!>/* suggested message: the same storage location should not be passed as multiple inout arguments in one call. source: manual_source_zh_cn_cangjie_c_inout_参数_3; docs/diagnostics-gap-vs-official-cpp-sema-detailed.md */swap(inout x, inout x)<!>
}
