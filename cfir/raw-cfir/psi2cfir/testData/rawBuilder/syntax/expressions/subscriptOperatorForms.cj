// RUN_PIPELINE_TILL: FRONTEND
// 下标读写：get（`operator func [](i: T)`）与 set（`operator func [](i: T, value!: T)`）同时存在时，赋值 / 复合赋值 / 自增自减都落到 set；
// 只有 get 时下标是只读表达式。注意：set 的第二个形参必须具名 `value!`，无名形参在调用处报 sema_unknown_named_argument（见 recovery）。
// 官方依据：Cangjie 文档《操作符重载》下标；官方 Lexer.cpp / Sema（C++ 互操作下标）；cjc 1.0.5 / 1.1.3 零诊断。
class M {
    private var values: Array<Int64> = [0, 0, 0]

    public init() {}

    public operator func [](i: Int64): Int64 {
        return values[i]
    }

    public operator func [](i: Int64, value!: Int64): Unit {
        values[i] = value
    }
}

class R {
    public init() {}

    public operator func [](i: Int64): Int64 {
        return i
    }
}

func subscriptOperatorForms(m: M, r: R, a: Array<Int64>): Int64 {
    m[0] = 1
    m[0] += 2
    m[0] -= 1
    m[0]++
    m[0]--
    a[0] += 1
    a[0]++
    let x = m[1]
    return x + r[0] + a[0]
}
