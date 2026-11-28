// 括号分组与复杂优先级组合：括号覆盖、嵌套括号与逻辑组合。
// 官方依据：Cangjie 文档《运算符》；cjc 1.0.5 / 1.1.3 零诊断。
func useParen(a: Int64, b: Int64, c: Bool, d: Bool): Int64 {
    let grouped = (1 + 2) * 3
    let nested = ((a + b) * (a - b)) / (a + 1)
    let logic = (c && d) || (!c && !d)
    let mixed = (a + b > 0 && !c) || d
    let _ = logic
    return grouped + nested
}
