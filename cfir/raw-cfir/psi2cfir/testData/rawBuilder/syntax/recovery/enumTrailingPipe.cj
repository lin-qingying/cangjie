// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：枚举体末尾多一个 `|`（官方 parse_expected_name "expected a enum value name after '|' in enum body, found '}'"，锚 `}`）。
// 官方 cjc 1.0.5 / 1.1.3：parse_expected_name。
enum TrailingPipe {
    | A
    | B |
}
