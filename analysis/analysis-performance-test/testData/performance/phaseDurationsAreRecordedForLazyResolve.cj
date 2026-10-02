package sample.performance

func helper(value: Int64): Int64 {
    return value + 1
}

func consume(value: Int64): Int64 {
    return helper(value)
}

main() {
}