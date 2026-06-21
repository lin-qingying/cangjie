

interface I0 {
    func foo(): Int64 {
        return 0
    }
}

class C0 <: I0 {
    public func foo2(): Int64 {
        var res: Int64 = super.<!NOT_MEMBER_OF!>foo1<!>()
        return res
    }
}

main(): Int64 {
    return 0
}
