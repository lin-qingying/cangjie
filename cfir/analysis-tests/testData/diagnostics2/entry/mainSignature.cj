// RUN_PIPELINE_TILL: FRONTEND
// 证据：官方 cjc 1.0.5 实测：`main(): Bool` → error: return type of 'main' is not 'Integer' or 'Unit'（锚 `main` 首字母 1 个字符，kind sema_unexpected_return_type_for_entry）。
// CFIR CfirMainFunctionSignatureChecker 同样 reportOn 在 main 函数名来源上，锚点一致。
<!UNEXPECTED_RETURN_TYPE_FOR_ENTRY!>m<!>ain(): Bool {
    return true
}
