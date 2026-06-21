

class A {
    <!RECURSIVE_CONSTRUCTOR_CALL!>init<!>() {
        A(1)
    }
    init(x: Int64) {
        A()
    }
}

main() {
    A()
    0
}
