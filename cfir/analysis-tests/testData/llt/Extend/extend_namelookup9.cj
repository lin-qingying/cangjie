


// struct cannot access member functions defined in its extend
enum E {
    A | B | C

    func foo() {
        <!UNRESOLVED_REFERENCE!>go<!>()
    }
}

extend E {
    func go() {}
}
