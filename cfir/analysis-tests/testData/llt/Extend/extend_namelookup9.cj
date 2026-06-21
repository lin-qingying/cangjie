


// struct cannot access member functions defined in its extend
enum E {
    A | B | C

    func foo() {
        <!UNRESOLVED_REFERENCE!>g<!>o()
    }
}

extend E {
    func go() {}
}
