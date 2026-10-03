// RUN_PIPELINE_TILL: FRONTEND
// 证据：官方 cjc 1.0.5 实测：
//   `let x = []` → error: array literal type cannot be inferred（锚 `[]`）
//     → 官方 kind sema_empty_arrayLit_type_undefined（include/cangjie/Basic/DiagnosticSema.def，CFIR 归一化为
//     ARRAY_LITERAL_TYPE_CANNOT_BE_INFERRED）
//   `(a, b) = (1, "x")`（a/b 已由 `(a, b) = (1, 2)` 推断为 Int64）→ error: mismatched types（锚整个右侧元组 `(1, "x")`；
//     另有 OtherHint 指向左值 `a`，不是独立诊断）→ 官方 kind sema_mismatched_types_multiple_assign
//     （DiagRefactor/DiagnosticSema.def:13），本项目对应 MISMATCHED_TYPES_MULTIPLE_ASSIGN。
//   对照：同类型元组赋值零诊断；右值元素顺序不同不改变官方锚整个右值元组的结论。
func arrayLiteralInference(): Unit {
    let inferred = <!ARRAY_LITERAL_TYPE_CANNOT_BE_INFERRED!>[<!>]
    let _ = inferred
}

func multipleAssignMismatch(): Unit {
    var (a, b) = (1, 2)
    (a, b) = <!MISMATCHED_TYPES_MULTIPLE_ASSIGN!>(1, "x")<!>
    let _ = (a, b)
}

func multipleAssignOk(): Unit {
    var (c, d) = (1, 2)
    (c, d) = (3, 4)
    (c, d) = <!MISMATCHED_TYPES_MULTIPLE_ASSIGN!>(4, "y")<!>
    let _ = (c, d)
}
