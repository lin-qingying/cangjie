// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：多余的右花括号。
// 官方 cjc 1.0.5 / 1.1.3：parse_expected_decl "expected declaration, found '}'"。
func strayCloseBrace(): Int64 {
    return 1
}
}
