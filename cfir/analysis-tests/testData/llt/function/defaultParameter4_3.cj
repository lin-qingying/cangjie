

func foo0(a: Int32, b!: Int32, c!: Int32 = 2): Unit {
    return
}

func foo1(arg0!: Int32, b!: Int32 = arg0 + 1): Unit {
    return
}

func foo2(a: Int32, b!: Int32 = a): Unit {
    return
}

main(): Int64 {
    foo0<!NO_VALUE_FOR_PARAMETER!>()<!>
    foo0<!NO_VALUE_FOR_PARAMETER!>(1)<!>
    foo0(1, <!NEED_NAMED_ARGUMENT!>2<!>)
    foo0(1, <!NEED_NAMED_ARGUMENT!>2<!>, 3)
    foo0(1, b: 2, c: 3, <!TOO_MANY_ARGUMENTS!>4<!>)
    foo1(<!NEED_NAMED_ARGUMENT!>1<!>)
    foo1(<!NEED_NAMED_ARGUMENT!>1<!>, 2)
    foo1(1, 2, <!TOO_MANY_ARGUMENTS!>3<!>)
    foo2(1)
    foo2(1, <!NEED_NAMED_ARGUMENT!>2<!>)
    foo2(1, 2, <!TOO_MANY_ARGUMENTS!>3<!>)
    return 0
}
