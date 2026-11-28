// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：使用不存在的 C 类型名 `CValue`（CFunc 的返回值类型需写成具体类型；CFunc 形参类型约束见 CFFICheck.cpp CheckCFuncParamType）。
// 官方 cjc 1.0.5 / 1.1.3：sema_undeclared_type_name 'CValue'。
func cfuncValueTypeName(g: CValue): Unit {
    let _ = g
}
