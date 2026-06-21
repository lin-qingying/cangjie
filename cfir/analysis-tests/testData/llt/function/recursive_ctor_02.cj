

class A {
    A() {
        A(true)
    }
    <!RECURSIVE_CONSTRUCTOR_CALL!>init<!>(_: Bool) {
        A()
    }
}

main() {}
