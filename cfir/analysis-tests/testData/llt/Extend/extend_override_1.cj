

interface I1 {
    func foo() {}
}

open class C1 {}

extend C1 <: I1 {}

class D1 <: C1 {
    public func <!EXTEND_FUNCTION_CANNOT_OVERRIDDEN!>foo<!>() {}
}

main() {}
