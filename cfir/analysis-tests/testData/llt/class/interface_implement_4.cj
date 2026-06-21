


interface I1 {
    func foo(): Int64 {
        return 1
    }
}

interface I2 {
    func foo(): Int64 {
        return 2
    }
}

open <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class C1<!> <: I1 & I2 {}

open class C2 <: I1 & I2 {
    public func foo(): Int64 {
        return 3
    }
}

main(): Int64 {
    var res = C1().<!AMBIGUOUS_FUNCTION_CALL!>foo<!>()
    if (res == 1) {
        return 0
    }
    return 1
}
