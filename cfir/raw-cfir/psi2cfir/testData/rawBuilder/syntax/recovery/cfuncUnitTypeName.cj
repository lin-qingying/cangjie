// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：使用不存在的 C 类型名 `CFuncUnit`（官方只有 CFunc / CType / CString / CPointer 这些 C 类型名）。
// 官方 cjc 1.0.5 / 1.1.3：sema_undeclared_type_name 'CFuncUnit'。
func cfuncUnitTypeName(g: CFuncUnit): Unit {
    let _ = g
}
