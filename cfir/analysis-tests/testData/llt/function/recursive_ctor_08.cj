

class B {
    init() {
        A()
    }
}

class A {
    <!RECURSIVE_CONSTRUCTOR_CALL!>init<!>() {
        B()
    }
}

main() {
    A()
    0
}
