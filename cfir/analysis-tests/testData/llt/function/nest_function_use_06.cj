

func foo<T>(a: T) {
    bar<Int64>(1)
    a
}

func bar<T>(a: T): T {
    baz(foo<Int64>)
    a
}

func baz<T>(a: T): T {
    a
}

main(): Int64 {
    let a = bar(1)
    if (a != 1) {
        return 1
    }
    return 0
}
