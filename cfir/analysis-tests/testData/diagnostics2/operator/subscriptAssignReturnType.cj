// RUN_PIPELINE_TILL: FRONTEND
// 证据：官方 cjc 1.0.5 实测：`operator func [](index: Int64, value!: Int64): Int64` →
// error: the return type of subscript assignment must be 'Unit'（锚返回类型 `Int64`）。
class Vec {
    private let data: Array<Int64> = [0]

    operator func [](index: Int64, value!: Int64): <!INVALID_SUBSCRIPT_ASSIGN_RETURN!>Int64<!> {
        this.data[index] = value
        return this.data[index]
    }
}
// 说明：官方 cjc 另有 `operator func [](index: Int64, v!: Int64): Unit`（具名参数不叫 value）→
// error: overloaded operator '[]' can only have one named parameter 'value'（kind sema_invalid_subscript_assign_parameter，
//   锚该具名参数 `v!`）；该形态在 CFIR 被折叠为下标读取运算符，见 subscriptAssignNamedParameter.cj。
