func foo<T>(a: T) {
    bar<Int64>(1)
    a
}

func bar<T>(a: T) {
    let b = foo<Int64>
    a
}

main(): Unit {
    bar(1)
}
