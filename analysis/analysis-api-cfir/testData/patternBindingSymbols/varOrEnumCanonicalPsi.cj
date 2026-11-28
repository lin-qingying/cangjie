// captured 没有同名 enum constructor，语义决议为真实绑定；规范 PSI 保留 VarOrEnumPattern。
package test.patterns.symbols

func captureNumber(value: Int64): Int64 {
    return match (value) {
        case captured => <caret_capturedUse>captured
    }
}
