

interface I0 {
    func f() {
        <!INVALID_THIS_CALL_OUTSIDE_CTOR!>this<!>()
    }
}

main(): Int64 {
    return 1
}
