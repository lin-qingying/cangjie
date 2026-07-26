

interface I1 {
    func a(a: Int32): Int32
}

interface I2 <: I1 {}

interface I3 <: I2 {}

class <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>A<!> <: I2 {
    public func a(): Int32 {
        1
    }
}
