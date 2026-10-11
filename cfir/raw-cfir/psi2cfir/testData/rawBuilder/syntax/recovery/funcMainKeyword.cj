// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：给 main 加 `func` 关键字（官方 ParserImpl::DiagIllegalFunc：parse_illegal_function_name "'main' declaration doesn't need 'func' keyword"，锚 `main` 首字母 1 字符）。
// 官方 cjc 1.0.5 / 1.1.3：parse_illegal_function_name。
func main(): Int64 {
    return 0
}
