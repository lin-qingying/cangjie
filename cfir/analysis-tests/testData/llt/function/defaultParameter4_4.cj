

func foo0(a: Int32, b!: Int32, c!: Int32 = 2): Unit {
    return
}

main(): Int64 {
    foo0<!WRONG_NUMBER_OF_ARGUMENTS!>(a: 1)<!>
    foo0(1, <!UNKNOWN_NAMED_ARGUMENT!>d<!>: 2)
    foo0(1, <!UNKNOWN_NAMED_ARGUMENT!>d<!>: 2, e: 3)
    foo0<!WRONG_NUMBER_OF_ARGUMENTS!>(1, d: 2, e: 3, f: 4)<!>

    return 0
}
