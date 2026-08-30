

// DIAGNOSTICS: -UNUSED_EXPRESSION

struct C {
    static init() {}
    static <!CONFLICTING_OVERLOADS!>init<!>() {
        1
    }
}

main() {
    return 0
}
