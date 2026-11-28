// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：enum 构造器之间缺少 `|` 分隔。
// 官方 cjc 1.0.5 / 1.1.3：parse_expected_decl "expected declaration, found 'B'"。
enum MissingPipe {
    A
    B(Int64)
}
