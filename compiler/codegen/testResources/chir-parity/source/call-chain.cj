func add(a: Int64, b: Int64): Int64 {
    return a + b
}

func twice(x: Int64): Int64 {
    return add(x, x)
}

func combine(x: Int64): Int64 {
    return add(twice(x), 1)
}

main(): Int64 {
    return combine(7)
}
