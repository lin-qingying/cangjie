


// class cannot access member functions defined in its extend
class A {
    func foo() {
        <!UNRESOLVED_REFERENCE!>go<!>()
    }
}

extend A {
    func go() {}
}
