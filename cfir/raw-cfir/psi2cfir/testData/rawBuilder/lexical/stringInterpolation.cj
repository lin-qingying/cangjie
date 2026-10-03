// RUN_PIPELINE_TILL: FRONTEND
// 插值字符串：${表达式} 的各种表达式形态（变量、成员、调用、索引、算术、多语句声明、嵌套字符串、多行字符串内插值）。
// 官方依据：Cangjie 文档《字符串类型》「插值字符串」；cjc 1.0.5 / 1.1.3 零诊断。
class Holder {
    var field: Int64 = 7
}

func compute(x: Int64): Int64 {
    return x * 2
}

func interpolation(name: String, h: Holder, xs: Array<Int64>, x: Int64): String {
    let plain = "no interpolation"
    let dollarOnly = "100$ and $ not followed by brace"
    let variable = "x = ${x}"
    let member = "field = ${h.field}"
    let call = "call = ${compute(x)}"
    let index = "first = ${xs[0]}"
    let arithmetic = "sum = ${x + 1 * 2}"
    let statements = "stmt = ${let y = x + 1; y * 2}"
    let nested = "nested = ${"inner ${x}"}"
    let escaped = "escaped \${x} literal"
    let multi = "${name}: ${x}, ${h.field}, ${compute(x)}"
    let inMultiline = """
        multi ${name} line ${x}
    """
    let _ = (plain, dollarOnly, variable, member, call, index, arithmetic, statements, nested, escaped, multi, inMultiline)
    return multi
}
