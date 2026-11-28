// RUN_PIPELINE_TILL: FRONTEND
// 类型检查与转换：`is`、`as`、`is?`、`as?` 对类层次（open 基类 + 子类）以及对 Any 的转换。
// 官方依据：Cangjie 文档《类型转换》；官方 Sema/TypeCheckExpr/IsAsExpr.cpp；cjc 1.0.5 / 1.1.3 零诊断。
open class A {
    public init() {}
}

class B <: A {
    public init() { super() }
}

func typeCheckForms(x: Any, a: A): Unit {
    let b = x is A
    let c = x as A
    let d = x is? A
    let e = a is B
    let g = x as? A
    let _ = (b, c, d, e, g)
}
