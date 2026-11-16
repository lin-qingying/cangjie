package sample.sig

func inner(a: Int64, b: Int64): Int64 {
    return a + b
}

func outer(x: Int64, y: Int64): Int64 {
    return x + y
}

func call(): Int64 {
    return outer(inner(1, 2), 3)
}
