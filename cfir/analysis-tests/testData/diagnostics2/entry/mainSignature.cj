// RUN_PIPELINE_TILL: FRONTEND
// 证据：官方 cjc 1.0.5 实测：`main(): Bool` → error: return type of 'main' is not 'Integer' or 'Unit'（锚 `main` 首字母 1 个字符，kind sema_unexpected_return_type_for_entry）。
// 范围口径：官方 GetBegin 窄锚（1 字符）按项目 Diagnostic Range Policy 不镜像；CFIR 与本文件标记
// 统一锚完整 `main` token（CfirMainFunctionSignatureChecker reportOn mainFunctionNameDiagnosticSource）。
<!UNEXPECTED_RETURN_TYPE_FOR_ENTRY!>main<!>(): Bool {
    return true
}
