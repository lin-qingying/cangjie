

interface I {
    func a(a: Int32): Int32
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class A<!> <: I {
    public func a(): Int32 {
        1
    }
}
