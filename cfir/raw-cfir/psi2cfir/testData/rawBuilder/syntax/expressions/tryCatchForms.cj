// RUN_PIPELINE_TILL: FRONTEND
// try 表达式的形态补充：catch 的自定义异常类型、catch Exception、catch + finally、try 作为表达式值。注意：资源语法 `try (r = expr)` 要求资源类型实现
// `Resource`（`isClosed` + `close`），见 tryResourceForms.cj。
// 官方依据：Cangjie 文档《异常处理》；官方 Sema/TypeCheckExpr/TryExpr.cpp；cjc 1.0.5 / 1.1.3 零诊断。
class MyError <: Exception {
    public init(msg: String) { super(msg) }
}

func tryCatchForms(): Int64 {
    let a = try {
        1
    } catch (e: MyError) {
        2
    }
    let b = try {
        3
    } finally {
        4
    }
    let c = try {
        5
    } catch (e: Exception) {
        6
    }
    return a + b + c
}
