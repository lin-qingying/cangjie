

open class C0 {
    public func foo(): Int64 {
        return 0
    }
}

open class C1 <: C0 {}

class C2 <: C1 {
    public func foo2(): Int64 {
        var res: Int64 = super.<!NOT_MEMBER_OF!>foo1<!>()
        return res
    }
}

main(): Int64 {
    return 0
}
