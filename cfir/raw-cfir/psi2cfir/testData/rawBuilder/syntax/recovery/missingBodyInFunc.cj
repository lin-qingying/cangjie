// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：顶层函数缺少函数体。
// 官方 cjc 1.0.5 / 1.1.3：parse_missing_body "body of function 'f' is missing"。
func missingBody(): Int64
