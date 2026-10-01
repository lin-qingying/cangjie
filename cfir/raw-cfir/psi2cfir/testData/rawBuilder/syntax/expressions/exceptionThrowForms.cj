// RUN_PIPELINE_TILL: FRONTEND
// 异常：自定义异常基类与派生类、throw 作为表达式（if 表达式的 else 分支）、作为语句、`Nothing` 返回类型函数与把 `Nothing` 表达式传给形参。
// 官方依据：Cangjie 文档《异常处理》；cjc 1.0.5 / 1.1.3 零诊断。
open class BaseErr <: Exception {
    public init(msg: String) {
        super(msg)
    }
}

class SubErr <: BaseErr {
    public init() {
        super("sub")
    }
}

func fail(): Nothing {
    throw Exception()
}

func consume(x: Nothing): Int64 {
    return 0
}

func exceptionThrowForms(x: Int64): Int64 {
    throw SubErr()
}

func exceptionThrowAsExpr(x: Int64): Int64 {
    let a = if (x > 0) { 1 } else { throw SubErr() }
    return a
}

func exceptionThrowNothing(): Int64 {
    return fail() + consume(fail())
}

func catchDerived(): Int64 {
    try {
        throw SubErr()
    } catch (e: BaseErr) {
        return 1
    }
}
