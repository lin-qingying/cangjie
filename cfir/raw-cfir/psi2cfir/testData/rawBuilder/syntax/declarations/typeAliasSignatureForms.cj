// RUN_PIPELINE_TILL: FRONTEND
// 类型别名用在签名位置：别名标量、别名函数类型、别名 Option 化容器（`?M`），以及别名在类 / 结构 / extend 的类型形参位置上。
// 官方依据：Cangjie 文档《类型别名》；cjc 1.0.5 / 1.1.3 零诊断。
import std.collection.*

type Id = Int64
type H = (Int64) -> Unit
type M = HashMap<Int64, String>
type Pair<A, B> = (A, B)

func typeAliasSignatureForms(a: Id, b: H, c: ?M, d: Pair<Int64, String>): Unit {
    b(a)
    let _ = (c, d)
}

class Holder<T> {
    var v: T

    public init(v: T) {
        this.v = v
    }
}

func typeAliasGeneric(a: Box<Id>, b: Holder<Id>): Int64 {
    return 1
}
