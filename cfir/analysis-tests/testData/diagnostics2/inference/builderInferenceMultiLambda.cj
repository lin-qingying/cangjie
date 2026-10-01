// RUN_PIPELINE_TILL: FRONTEND
// 证据：官方 cjc 1.0.5 / 1.1.3 实测（kind sema_unable_to_infer_generic_func，锚被调用者名）：
//   `pair(1, "x")`（`pair<T>(a: T, b: T)`，两个实参推导出的 T 冲突）
//     → error: unable to infer generic argument of this function（锚 `pair`）。
//   同类型实参（`pair(1, 2)`）零诊断。
// 官方实现 TypeArgumentInference.cpp::ReportUnresolvedGenericFunc：诊断建在被调用者 `ce.baseFunc` 上。
//   官方对"多个 lambda 实参推导冲突"（builder 风格泛型函数）给的是 sema_invalid_node_after_check
//   （"semantic error" + note "please report this to Cangjie team"），是编译器内部断言失败而非设计的诊断，
//   位置为零宽，无法用内联标记表达，故本用例用同族但有设计诊断的实参冲突形态。
// 本项目：CFIR 对该形态报 UNABLE_TO_INFER_GENERIC_FUNC。
func pair<T>(a: T, b: T): T {
    return a
}

func conflictingTypeArguments(): Unit {
    <!UNABLE_TO_INFER_GENERIC_FUNC!>pair<!>(1, "x")
}

func consistentTypeArguments(): Unit {
    let _ = pair(1, 2)
}
