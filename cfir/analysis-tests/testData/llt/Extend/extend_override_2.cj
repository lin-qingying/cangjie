

interface I2 {
    func foo(): Unit
}

open class C2 {}

extend C2 <: I2 {
    public func foo() {}
}

class D2 <: C2 {
    public func <!EXTEND_FUNCTION_CANNOT_OVERRIDDEN!>foo<!>() {}
}

main() {}
