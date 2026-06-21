

class A {
    <!RECURSIVE_CONSTRUCTOR_CALL!>init<!>() {
        A(true)
    }
    init(_: Bool) {
        A()
    }
}

main() {}
