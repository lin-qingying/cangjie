// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：VArray 构造调用的具名实参名不是 repeat（官方 ChkVArrayArg 只接受 repeat / 无名 lambda 实参）。
// 官方 cjc 1.0.5 / 1.1.3：sema_unknown_named_argument（官方源码附注写的是 'item'，实消息不带附注，按实消息记录）。
func badName(): Unit {
    let a = VArray<Int64, $2>(item: 0)
    let _ = a
}
