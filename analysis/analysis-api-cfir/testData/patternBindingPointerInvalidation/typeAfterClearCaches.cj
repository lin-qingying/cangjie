package test.patterns.pointers

func typedValue(value: Int64): Int64 {
    return match (value) {
        case <caret_typed>typed: Int64 => <caret_typedUse>typed
    }
}

main() { println(typedValue(7)) }
