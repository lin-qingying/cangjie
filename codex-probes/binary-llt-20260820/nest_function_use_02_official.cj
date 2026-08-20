func foo<T>(a: T) {
    bar<Int64>(1)
    a
}

func bar<T>(a: T) {
    let b = {a: Any => ()}(foo<Int64>)
    a
}

func baz<T>(a: T): T {
    a
}

main(): Unit {
    bar(1)
}
