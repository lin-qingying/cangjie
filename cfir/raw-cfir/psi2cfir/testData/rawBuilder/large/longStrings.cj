// RUN_PIPELINE_TILL: FRONTEND
// 大文件场景：超长单行字符串、40 行多行字符串、30 个插值点的模板字符串、300 字符的原始字符串与大量拼接，
// 用于覆盖词法层的字符串缓冲、插值分段（StringPart）与构树时的字符串模板节点。cjc 1.0.5 / 1.1.3 零诊断。
func longStrings(n: Int64): String {
    let single = "abcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabc"
    let multi = """
line 001: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
line 002: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
line 003: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
line 004: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
line 005: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
line 006: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
line 007: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
line 008: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
line 009: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
line 010: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
line 011: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
line 012: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
line 013: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
line 014: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
line 015: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
line 016: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
line 017: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
line 018: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
line 019: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
line 020: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
line 021: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
line 022: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
line 023: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
line 024: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
line 025: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
line 026: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
line 027: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
line 028: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
line 029: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
line 030: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
line 031: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
line 032: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
line 033: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
line 034: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
line 035: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
line 036: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
line 037: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
line 038: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
line 039: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
line 040: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
    """
    let template = "sum ${n + 1} ${n + 2} ${n + 3} ${n + 4} ${n + 5} ${n + 6} ${n + 7} ${n + 8} ${n + 9} ${n + 10} ${n + 11} ${n + 12} ${n + 13} ${n + 14} ${n + 15} ${n + 16} ${n + 17} ${n + 18} ${n + 19} ${n + 20} ${n + 21} ${n + 22} ${n + 23} ${n + 24} ${n + 25} ${n + 26} ${n + 27} ${n + 28} ${n + 29} ${n + 30} end"
    let raw = #"
rrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrr
"#
    var acc = single
    acc += multi
    acc += template
    acc += raw
    for (i in 0..10) {
        acc += "-" + i.toString()
    }
    return acc
}
