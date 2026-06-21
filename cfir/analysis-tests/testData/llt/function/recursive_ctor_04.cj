

open class B {
    var a: A = A()
}

class <!RECURSIVE_CONSTRUCTOR_CALL!>A<!> <: B {}

main() {
    let _ = A()
}
