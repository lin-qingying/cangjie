


func foo<T>(a: T, b: T) {
    return (a, b)
}

func bar<T>(a: T, b: T) {
    let (x, y) = foo<T>(a, b)
    return (x, y)
}

main() {
    let (x, y) = bar<Int32>(1, 2)
    return 0
}
