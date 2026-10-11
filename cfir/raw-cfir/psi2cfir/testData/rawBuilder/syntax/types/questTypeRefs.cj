// RUN_PIPELINE_TILL: FRONTEND
// 可选类型引用：?T、Array<?T>?>、?(T1, T2)、?((T) -> U)、?(T) -> U（官方 1.0.5 / 1.1.3 均接受），以及 Option 泛型别名。
// 官方依据：Cangjie 文档《Option 类型》《操作符》；cjc 1.0.5 / 1.1.3 零诊断（postfix T? 是官方 parse 错误，见 diagnostics2/general/coreObjectNotFoundNoPrelude.cj）。
func optInt(a: ?Int64): ?Int64 { return a }
func optArray(a: Array<?Int64>): ?Array<Int64> { return None }
func optTuple(a: ?(Int64, String)): ?(Int64, String) { return None }
func optFuncParen(a: ?((Int64) -> Unit)): ?((Int64) -> Unit) { return a }
func optFuncBare(a: ?(Int64) -> Unit): ?(Int64) -> Unit { return a }
func optNested(a: ?Int64): ?Int64 { return a }

type Maybe<T> = ?T
type MaybeList = Array<Maybe<Int64>>

func questTypeUses(): Unit {
    let a: Maybe<Int64> = None
    let b: MaybeList = [None, Some(1)]
    let c = Some<Int64>(2)
    let _ = (a, b, c)
}
