


// struct cannot access member functions defined in its extend
struct A {
    func foo() {
        <!UNRESOLVED_REFERENCE!>g<!>o()
    }
}

extend A {
    func go() {}
}
