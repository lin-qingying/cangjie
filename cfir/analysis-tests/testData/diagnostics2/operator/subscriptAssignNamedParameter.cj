// RUN_PIPELINE_TILL: FRONTEND
// DISABLE_WITH_PARSER: Psi
// 证据：官方 cjc 1.0.5 / 1.1.3 实测：`operator func [](index: Int64, v!: Int64): Unit`（下标赋值的具名参数不叫 `value`）
//   → error: overloaded operator '[]' can only have one named parameter 'value'
//     （kind sema_invalid_subscript_assign_parameter，锚该具名参数 `v!` 2 个字符）
// 本项目：官方 kind 对应 INVALID_SUBSCRIPT_ASSIGN_PARAMETER；CFIR 只在具名参数名为 `value` 时把
//   `operator func []` 识别为下标赋值运算符，本形态被折叠为读取运算符（实现缺口）。
// 本用例对 PSI 入口关闭的原因见 subscriptAssignNoPositional.cj（PSI 解析路径在该签名上抛声明检查器异常）。
class Vec {
    private let data: Array<Int64> = [0]

    operator func [](index: Int64, <!INVALID_SUBSCRIPT_ASSIGN_PARAMETER!>v<!>!: Int64): Unit {
        this.data[index] = v
    }
}
