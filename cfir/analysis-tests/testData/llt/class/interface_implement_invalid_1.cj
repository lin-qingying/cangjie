

interface I1 {
    func foo(): Int64
}

interface I2 <: I1 {}

interface C1 <: I2 {}

class <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>C2<!> <: I2 {}

class <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>C3<!> <: I2 {
    public func foo(a: Int64): Int64 {
        return 0
    }
}

class C4 <: I2 {
    public func foo(): Int64 {
        return 0
    }
}

main(): Int64 {
    return 0
}
