// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：使用 J 字符串字面量 `J"..."`（官方 Lexer.cpp `ScanStringOrJString`、`ParseAtom.cpp` `StringKind::JSTRING`），但 1.0.5 / 1.1.3 的
// SDK 没有 `JString` / `JsonElement` 类型（std.json 包不存在），报 sema_undeclared_type_name。
// 官方 cjc 1.0.5 / 1.1.3：sema_undeclared_type_name 'JString'。
func jstringUpperCase(): Unit {
    let a = J"{}"
    let _ = a
}
