// RUN_PIPELINE_TILL: FRONTEND
// 嵌套类型引用：元组里套元组 `(Int64, (String, Bool))`、数组元素是元组 `Array<(Int64, Array<String>)>`、数组元素是 Option `Array<?Int64>`、
// `CType` 作为形参类型。
// 官方依据：Cangjie 文档《类型引用》；cjc 1.0.5 / 1.1.3 零诊断。
func nestedTypeRefForms(
    a: (Int64, (String, Bool)),
    b: Array<(Int64, Array<String>)>,
    c: Array<?Int64>,
    d: CType,
    e: CType,
): Int64 {
    let _ = (a, b, c, d, e)
    return c[0] ?? 0
}
