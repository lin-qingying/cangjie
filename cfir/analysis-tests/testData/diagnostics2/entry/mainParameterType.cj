// RUN_PIPELINE_TILL: FRONTEND
// 证据：官方 cjc 1.0.5 实测：`main(a: Int64, b: Int64): Int64` → error: 'main' cannot be defined with parameter whose type is not 'Array<String>'（锚 `main` 首字母 1 个字符，kind sema_unexpected_param_for_entry）。
// 合法对照：`main(args: Array<String>): Int64` 零诊断。
func notMain(a: Int64, b: Int64): Int64 {
    return a + b
}

<!UNEXPECTED_PARAM_FOR_ENTRY!>m<!>ain(a: Int64, b: Int64): Int64 {
    return a + b
}
