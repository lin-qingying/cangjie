// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：foreign 函数用 `CArray<Any>` 形参（官方没有 CArray 类型名；可变参数在文档里写作 `args: CArray<Any>` 的形式在 1.0.5 / 1.1.3 不存在）。
// 官方 cjc 1.0.5 / 1.1.3：sema_undeclared_type_name 'CArray'。
foreign func native(fmt: CString, args: CArray<Any>): Int64
