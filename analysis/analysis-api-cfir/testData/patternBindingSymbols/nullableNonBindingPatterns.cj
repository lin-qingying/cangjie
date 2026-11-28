// Ready 是 constructor 使用，_: Int64 是无绑定的类型模式；二者 bindingSymbol 均为 null。
package test.patterns.symbols

enum Choice {
    | Ready
    | Waiting
}

func isReady(value: Choice): Bool {
    return match (value) {
        case <caret_constructorUse>Ready => true
        case _ => false
    }
}

func isNumber(value: Any): Bool {
    return match (value) {
        case _: Int64 => true
        case _ => false
    }
}
