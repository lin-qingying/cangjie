

func foo0(a: Int32, b!: Int32 = 2, c!: Int32): Unit {
    return
}

main(): Int64 {
    foo0<!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!>()<!>
    foo0<!NO_VALUE_FOR_PARAMETER!>(1)<!>
    foo0(1, <!NEED_NAMED_ARGUMENT!>2<!>)
    foo0(1, <!NEED_NAMED_ARGUMENT!>2<!>, 3)
    foo0(1, b: 2, c: 3, <!TOO_MANY_ARGUMENTS!>4<!>)
    return 0
}
