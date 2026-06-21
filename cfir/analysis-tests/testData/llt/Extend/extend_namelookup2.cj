


// class cannot access member functions defined in its extend
class A {
    func foo() {
        <!UNRESOLVED_REFERENCE!>g<!>o()
    }
}

extend A {
    func go() {}
}
