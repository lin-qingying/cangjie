

func foo0(a: Int32, b!: Int32 = <!UNRESOLVED_REFERENCE!>c<!> + 1, c!: Int32 = a * b) {
    c
}

func foo1(arg0!: Int32, b!: Int32 = arg0 + 1) {
    b
}

func foo2(a: Int32, b!: Int32 = <!UNRESOLVED_REFERENCE!>b<!>) {
    <!UNRESOLVED_REFERENCE!>c<!>
}

func run(): Int64 {
    foo1<!WRONG_NUMBER_OF_ARGUMENTS!>(1)<!>
    return 1
}

main(): Int64 {
    return run()
}
