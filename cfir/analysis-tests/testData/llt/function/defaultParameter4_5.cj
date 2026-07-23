

func foo0(a: Int32, b!: Int32 = 2, c!: Int32): Unit {
    return
}

main(): Int64 {
    foo0<!WRONG_NUMBER_OF_ARGUMENTS!>()<!>
    foo0<!WRONG_NUMBER_OF_ARGUMENTS!>(1)<!>
    foo0<!WRONG_NUMBER_OF_ARGUMENTS!>(1, 2)<!>
    foo0(1, <!NEED_NAMED_ARGUMENT!>2<!>, 3)
    foo0<!WRONG_NUMBER_OF_ARGUMENTS!>(1, b: 2, c: 3, 4)<!>
    return 0
}
