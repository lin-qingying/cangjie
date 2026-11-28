// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：枚举体里用 `...` 表示非穷尽（官方 1.0.5 / 1.1.3 的 ParseEnumBody 不接受 `...`，报 parse_expected_decl "expected declaration, found '...'"；
// match 侧用兜底分支 `case _` 表达非穷尽）。
// 官方 cjc 1.0.5 / 1.1.3：parse_expected_decl。
enum NonExhaustive {
    | A
    | B
    ...
}
