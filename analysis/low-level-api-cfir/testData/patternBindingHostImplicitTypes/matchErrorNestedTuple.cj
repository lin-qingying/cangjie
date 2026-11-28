// selector 的原始错误须传播到每个 active leaf，嵌套层也不得留下 implicit。
// 逐叶 ConeErrorType 收敛是项目契约，不声称官方在错误后逐叶写入类型。
package test

func matchTupleHost(): Unit {
    match (unresolvedSelectorForPattern) {
        case (number, (flag, text)) => ()
        case _ => ()
    }
    ()
}
