// RUN_PIPELINE_TILL: FRONTEND
// 幂运算 `**`：Int64 ** UInt64、链式不可用（右结合的 `a ** b ** c` 与 `a ** (b ** c)` 都报 sema_invalid_binary_expr '**' on type 'UInt64' and 'UInt64'）、
// 与其它运算符混用、以及 `operator func **` 重载（可重载，官方 BUILTIN_OPERATORS 含 `**`）。
// 官方依据：Cangjie 文档《操作符》；官方 Sema/TypeCheckUtil.cpp BUILTIN_OPERATORS；cjc 1.0.5 / 1.1.3 零诊断。
func powerOperatorForms(a: Int64, b: UInt64): Int64 {
    let p = a ** b
    let q = a ** b + a
    let r = (a + 1) ** b
    return p + q + r
}

class P {
    public init() {}

    public operator func ** (o: P): P {
        return o
    }
}

func powerOverload(x: P, y: P): P {
    return x ** y
}
