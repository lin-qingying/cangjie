// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：@When 的条件列表为空。
// 官方 cjc 1.0.5 / 1.1.3：parse_expected_expression "expected expression after '[', found ']'"。
@When[]
func emptyWhen(): Int64 {
    return 1
}
