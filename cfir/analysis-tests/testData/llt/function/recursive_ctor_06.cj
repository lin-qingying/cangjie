

open class A {
    let b = B()
}

class B <: A {
    <!RECURSIVE_CONSTRUCTOR_CALL!>B<!>() {
        super() // the super should be in the dependency path
    }
}

main() {
    let _ = A()
}
