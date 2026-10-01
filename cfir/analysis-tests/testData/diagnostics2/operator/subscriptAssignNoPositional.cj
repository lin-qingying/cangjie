// RUN_PIPELINE_TILL: FRONTEND
// DISABLE_WITH_PARSER: Psi
// 证据：官方 cjc 1.0.5 实测：
//   `operator func [](value!: Int64): Unit` → error: overloaded operator '[]' should have at least one positional parameter for index
//     （kind sema_invalid_subscript_assign_parameter_num，锚运算符名 `[]`）
//   `operator func [](index: Int64, v!: Int64): Unit`（具名参数不叫 value）→ error: overloaded operator '[]' can only have one named parameter 'value'
//     在 CFIR 中该形态被折叠为下标读取运算符，不触发 INVALID_SUBSCRIPT_ASSIGN_PARAMETER（见 subscriptAssignReturnType.cj 注释）。
// 本项目：官方 kind 对应 INVALID_SUBSCRIPT_ASSIGN_PARAMETER_NUM，CFIR 锚在 `operator` 修饰符上（LightTree 入口），与官方锚点不同（实现缺口）。
// 本用例对 PSI 入口关闭：PSI 解析路径在该签名上抛 CangJieIllegalArgumentExceptionWithAttachments（声明检查器异常），
// 属于 PSI / LightTree 前端行为差异，待 PSI 侧修复后恢复。
class Vec {
    private let data: Array<Int64> = [0]

    operator func <!INVALID_SUBSCRIPT_ASSIGN_PARAMETER_NUM!>[]<!>(value!: Int64): Unit {
        this.data[0] = value
    }
}
