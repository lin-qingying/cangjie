// RUN_PIPELINE_TILL: FRONTEND
// 证据：官方 cjc 1.0.0 / 1.1.3 实测：`main(a: Int64, b: Int64): Int64` → error: 'main' cannot be defined with parameter whose type is not 'Array<String>'（kind sema_unexpected_param_for_entry，官方位置落在 `main` 首字符）。
// 合法对照：`main(args: Array<String>): Int64` 零诊断。
// CFIR 锚定完整 `main` 名称 token；不得裁剪为首字符。
func notMain(a: Int64, b: Int64): Int64 {
    return a + b
}

<!UNEXPECTED_PARAM_FOR_ENTRY!>main<!>(a: Int64, b: Int64): Int64 {
    return a + b
}
