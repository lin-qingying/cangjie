

class A {
    let b = B()
}

class B {
    let a = <!RECURSIVE_CONSTRUCTOR_CALL!>A()<!>
}

main() {
    let _ = A()
}
