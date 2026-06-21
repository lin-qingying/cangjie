

interface I1 {
    func a(a: Int32): Int32
}

interface I2 <: I1 {
    func a(): Int32
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class A<!> <: I1 & I2 {
    public func a(): Int32 {
        1
    }
}

main() {
    var res = A().a()
    if (res == 1) {
        return 0
    }
    return 1
}
