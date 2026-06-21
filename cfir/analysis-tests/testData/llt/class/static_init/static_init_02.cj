


class C {
    <!CONFLICTING_OVERLOADS!>static init()<!> {}
    <!CONFLICTING_OVERLOADS!>static init()<!> {
        1
    }
}

main() {
    return 0
}
