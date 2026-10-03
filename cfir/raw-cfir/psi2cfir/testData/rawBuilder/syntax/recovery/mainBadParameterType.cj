// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：main 的形参类型不是 Array<String>（官方 sema_unexpected_param_for_entry "'main' cannot be defined with parameter whose type is not 'Array<String>'"，锚 `main` 首字母 1 字符）。
// 官方 cjc 1.0.5 / 1.1.3：sema_unexpected_param_for_entry。
main(args: Int64): Int64 {
    return args
}
