// 官方 `ParserImpl::ParseIfAvailable` 语法：`@IfAvailable(<name>: <literal>, { lambda }, { lambda })`。
// cjc 1.0.5 对该表达式能给出 "import 'ohos.device_info' to use the 'IfAvailable' expression"，
// 说明语法层解析通过（后续诊断属于环境/APILevel 语义，不影响 raw-cfir 转换）。
func ifAvailableDemo(): Int64 {
    @IfAvailable(level: 12, { => 1 }, { => 2 })
    return 1
}
