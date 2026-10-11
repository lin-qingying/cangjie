// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：使用 `Result<T, E>` 与 `Ok` / `Err`（1.0.5 / 1.1.3 的 std 没有 result 包也没有 Result 类型）。
// 官方 cjc 1.0.5 / 1.1.3：sema_undeclared_type_name 'Result' / sema_undeclared_identifier 'Ok'。
func resultTypeUndeclared(): Result<Int64, String> {
    return Ok(1)
}
