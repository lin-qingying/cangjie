// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：main 带多个形参列表（柯里化）（官方 ParseDecl.cpp ParseMain："The Curring function is not supported"，报 parse_expected_left_brace，锚第二个 `(`）。
// 官方 cjc 1.0.5 / 1.1.3：parse_expected_left_brace。
main(args: Array<String>)(): Int64 {
    return 0
}
