// RUN_PIPELINE_TILL: FRONTEND
// 证据：官方 cjc 1.0.5 实测：`extend Int64 { operator func [](index: Int64, value!: Int64): Unit {} }` →
// error: it's illegal to extend index assignment operator '[](index, value)' for immutable type（锚 `[]` 运算符名）。
// 锚点差异：cjc 锚在 `[]`；CFIR CfirExtendImmutableMemberChecker 锚在整条 operator 函数声明上。
// 合法对照：struct 是可变目标，同样的 `[]=` 重载零诊断。
extend Int64 {
    operator func <!EXTEND_IMMUTABLE_INDEX_ASSIGNMENT!>[]<!>(index: Int64, value!: Int64): Unit {}
}

struct Mutable {}

extend Mutable {
    operator func [](index: Int64, value!: Int64): Unit {}
}
