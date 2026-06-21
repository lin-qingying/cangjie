

main(): Int64 {
    if (foo3() != 6) {
        return 6
    }
    return 0
}

func foo(x: (Int32) -> Int32) {
    return x
}

func foo0(x: Int32) {
    return x + 1
}

func foo3() {
    func foo0(x: Int32) {
        return x + 2
    }
    let a = foo0
    let b = foo(foo0)
    return a(1) + b(1)
}
