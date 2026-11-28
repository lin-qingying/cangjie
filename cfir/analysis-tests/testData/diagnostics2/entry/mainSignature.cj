// RUN_PIPELINE_TILL: FRONTEND
// 证据：官方 cjc 1.0.0 / 1.1.3 实测：`main(): Bool` → error: return type of 'main' is not 'Integer' or 'Unit'（kind sema_unexpected_return_type_for_entry，官方位置落在 `main` 首字符）。
// CFIR CfirMainFunctionSignatureChecker 锚定完整 `main` 名称 token；不得裁剪为首字符。
<!UNEXPECTED_RETURN_TYPE_FOR_ENTRY!>main<!>(): Bool {
    return true
}
