// 可选类型引用：?T 在形参、返回、数组元素、元组与函数类型中的位置。
// 官方依据：Cangjie 文档《Option 类型》；cjc 1.0.5 / 1.1.3 零诊断。
func optInt(a: ?Int64): ?Int64 {
    return a
}

func optArray(a: Array<?Int64>): ?Array<Int64> {
    return None
}

func optTuple(a: ?(Int64, String)): ?(Int64, String) {
    return None
}

func optFuncParen(a: ?((Int64) -> Unit)): ?((Int64) -> Unit) {
    return a
}

func optFuncBare(a: ?(Int64) -> Unit): ?(Int64) -> Unit {
    return a
}

func optNested(a: ?Int64): ?Int64 {
    return a
}

type Maybe<T> = ?T
type MaybeList = Array<Maybe<Int64>>

func questTypeUses(): Unit {
    let a: Maybe<Int64> = None
    let b: MaybeList = []
    let _ = (a, b)
}
