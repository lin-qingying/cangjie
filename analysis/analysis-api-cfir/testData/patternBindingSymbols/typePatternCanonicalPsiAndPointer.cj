// 类型绑定使用显式 Int64，并以完整 number: Int64 节点作为规范 PSI。
package test.patterns.symbols

func extractNumber(value: Any): Int64 {
    return match (value) {
        case number: Int64 => <caret_typedUse>number
        case _ => 0
    }
}
