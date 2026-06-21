

func foo<T>() {
    1
}

func me<T>(a!: () -> Int64 = <!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>foo<!>) {
    0
}

main(): Int64 {
    return me<Int64>() - 1
}
