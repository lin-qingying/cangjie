// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：不 `import std.collection.*` 直接用 `Map` / `HashSet` / `ArrayList` / `Result` 类型名。
// 官方 cjc 1.0.5 / 1.1.3：sema_undeclared_type_name。
func collectionTypeNoImport(m: Map<Int64, String>, s: HashSet<Int64>, l: ArrayList<Int64>): Int64 {
    return m.size + s.size + l.size
}
