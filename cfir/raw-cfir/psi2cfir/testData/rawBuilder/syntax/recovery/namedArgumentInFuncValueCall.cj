// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：调用函数值（形参类型是函数类型）时用具名实参（官方 Sema/TypeCheckCall.cpp CheckCallArgs 对函数值调用报
// sema_unsupport_named_argument "named argument cannot be used in variable function call"）。
// 官方 cjc 1.0.5 / 1.1.3：2 条 sema_unsupport_named_argument。
func callWithNamedArgs(g: (x: Int64, y: String) -> Bool): Bool {
    return g(x: 1, y: "x")
}
