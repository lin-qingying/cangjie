


class A {
    static init() {
        if (1 > 2) {
            <!INVALID_RETURN_IN_STATIC_INIT!>return<!>
        } else {
            <!INVALID_RETURN_IN_STATIC_INIT!>return<!>
        }
    }
}

main() {
    return 0
}
