


class C {
    static init() {}
    static <!CONFLICTING_OVERLOADS!>init<!>() {
        1
    }
}

main() {
    return 0
}
