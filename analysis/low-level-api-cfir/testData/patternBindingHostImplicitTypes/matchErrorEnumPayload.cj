// enum payload 的 active binders 也须在 selector 错误的 early return 前完成错误收敛。
package test

enum Payload<T> {
    | Value(T, Bool)
    | Empty
}

func matchEnumHost(): Unit {
    match (unresolvedSelectorForPattern) {
        case Value(payload, flag) => ()
        case _ => ()
    }
    ()
}
