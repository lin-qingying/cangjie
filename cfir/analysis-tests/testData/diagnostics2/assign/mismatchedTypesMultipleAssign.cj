// RUN_PIPELINE_TILL: FRONTEND
// 证据：官方 cjc 1.0.5 / 1.1.3 实测：`(a, b) = ("x", 1)`（`a: Int64`、`b: Bool`）→
//   kind sema_mismatched_types_multiple_assign，message "mismatched types"，
//   note "the expression has type 'Tuple<Struct-String, Int64>'；主锚是整个右值元组，OtherHint 指向左值 `a`。
// 本项目：官方 kind 对应 MISMATCHED_TYPES_MULTIPLE_ASSIGN（锚右值元组）。
func multiAssign(): Unit {
    var a = 0
    var b = false
    (a, b) = <!MISMATCHED_TYPES_MULTIPLE_ASSIGN!>("x", 1)<!>
}
