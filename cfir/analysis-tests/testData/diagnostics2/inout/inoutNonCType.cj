// RUN_PIPELINE_TILL: FRONTEND
// 证据：官方 cjc 1.0.5 / 1.1.3 实测：`swap(inout s, 1)`（`s` 是 String）→ 两条诊断：
//   kind sema_inout_modify_non_ctype "the type of experssion qualified by 'inout' must meet 'CType' constraint"
//     （官方文案自带拼写错误 experssion），锚 `inout s` 整段；
//   kind sema_mismatched_types "mismatched types"（MainHint expected 'Int64', found 'Struct-String'），锚 `s`。
// 官方两条锚点不同：非 CType 锚 `inout s` 整段，mismatched types 锚 `s`（形参类型 Int64 vs 实参 Struct-String），
//   故本用例用嵌套标记：外层 INOUT_MODIFY_NON_CTYPE 覆盖 `inout s`，内层 TYPE_MISMATCH 覆盖 `s`。
foreign func swap(value: Int64, other: Int64): Unit

func useString(): Unit {
    var s = "x"
    unsafe {
        swap(<!INOUT_MODIFY_NON_CTYPE!>inout <!TYPE_MISMATCH!>s<!><!>, 1)
    }
}
