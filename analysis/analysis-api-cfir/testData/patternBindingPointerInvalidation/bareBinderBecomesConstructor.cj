package test.patterns.pointers

enum Choice { | Seed }

func classify(value: Choice): Int64 {
    return match (value) {
        case <caret_flip>Captured => 1
        case _ => 2
    }
}

main() { println(classify(Seed)) }
