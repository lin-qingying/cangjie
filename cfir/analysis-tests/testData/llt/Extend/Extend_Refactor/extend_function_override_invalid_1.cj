


open class C1 {}

extend C1 {
    public func foo(): Int64 {
        0
    }
}

class C2 <: C1 {
    public func <!EXTEND_FUNCTION_CANNOT_OVERRIDDEN!>foo<!>(): Int64 {
        0
    }
}

main() {
    return -1
}
