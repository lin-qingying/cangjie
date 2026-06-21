

func <!UNABLE_TO_INFER_RETURN_TYPE!>foo<!><T>(a: T) {
    bar<Int64>(1)
    a
}

func bar<T>(a: T) {
    baz(foo<Int64>(1)) // Use unknown as function call
    a
}

func baz<T>(a: T): T {
    a
}

main(): Unit {
    bar(1)
}
