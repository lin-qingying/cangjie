


// extend member functions cannot be shadowed by it sub classes
open class A {}

extend A {
    func foo() {}
}

class B <: A {
    func <!EXTEND_FUNCTION_CANNOT_OVERRIDDEN!>foo<!>() {}
}
