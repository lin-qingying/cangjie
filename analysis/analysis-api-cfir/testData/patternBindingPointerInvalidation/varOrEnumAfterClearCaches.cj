package test.patterns.pointers

func capture(value: Int64): Int64 {
    return match (value) {
        case <caret_captured>captured => <caret_capturedUse>captured
    }
}

main() { println(capture(7)) }
