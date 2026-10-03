// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：C 互操作里用 `CChar` 类型名（1.0.5 / 1.1.3 没有 `CChar`；CChar 的替代是 `UInt8` / `CString`）。
// 官方 cjc 1.0.5 / 1.1.3：sema_undeclared_type_name 'CChar'。
func ccharTypeUndeclared(a: CPointer<CChar>): Unit {
    let _ = a
}
