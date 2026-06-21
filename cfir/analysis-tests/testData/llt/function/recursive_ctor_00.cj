

class A {
    let a = <!RECURSIVE_CONSTRUCTOR_CALL!>A()<!>
}

main() {}
