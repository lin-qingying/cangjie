package test.patterns.pointers

func classify(value: Int64): Int64 {
    return match (value) {
        case <caret_erased>erased => 1
    }
}

main() { println(classify(7)) }
