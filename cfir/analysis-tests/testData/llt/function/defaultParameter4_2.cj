

func foo(a!: Int32 = 1, b!: Int32 = 1) {
    return a + b
}

main() {
    var x = foo
    var res = x(<!UNSUPPORTED_NAMED_ARGUMENT!>a<!>: 2, <!UNSUPPORTED_NAMED_ARGUMENT!>b<!>: 2)
    if (res != 4) {
        return 1
    }
    return 0
}
