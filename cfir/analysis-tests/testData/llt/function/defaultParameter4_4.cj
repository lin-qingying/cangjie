

func foo0(a: Int32, b!: Int32, c!: Int32 = 2): Unit {
    return
}

main(): Int64 {
    foo0(<!NAMED_ARGUMENTS_NOT_ALLOWED!>a<!>: 1)
    foo0(1, <!NAMED_PARAMETER_NOT_FOUND!>d<!>: 2)
    foo0(1, <!NAMED_PARAMETER_NOT_FOUND!>d<!>: 2, e: 3)
    foo0(1, d: 2, e: 3, <!TOO_MANY_ARGUMENTS!>f: 4<!>)

    return 0
}
