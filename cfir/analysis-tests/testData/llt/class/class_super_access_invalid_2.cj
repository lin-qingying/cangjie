

open class C1 {
    public func foo(): Int64 {
        return 0
    }
}

interface I1 {
    func foo(): Int64 {
        return 0
    }
}

class C2 <: C1 & I1 {
    public func foo1(): Int64 {
        var f = super.<!NOT_MEMBER_OF!>noexist<!>
        return 0
    }
}

main(): Int64 {
    return 1
}
