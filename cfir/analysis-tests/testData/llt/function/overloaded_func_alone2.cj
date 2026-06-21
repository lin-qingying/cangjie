

func foo(x: Int64) {
    return x
}

func foo(x: Float64) {
    return Int64(x)
}

main(): Int64 {
    var a: (Int64) -> Int64 = foo
    return a(0)
}
