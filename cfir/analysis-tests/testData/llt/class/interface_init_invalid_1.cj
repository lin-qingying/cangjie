

interface I0 {
    func f() {
        <!ILLEGAL_THIS_OR_SUPER_CALL!>this<!>()
    }
}

main(): Int64 {
    return 1
}
