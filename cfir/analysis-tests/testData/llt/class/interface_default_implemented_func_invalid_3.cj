


interface I1 {
    func foo1(): Int64 {
        return 0
    }
}

struct R1 <: I1 {}

extend R1 {
    public func <!EXTEND_MEMBER_CANNOT_SHADOW!>foo1<!>(): Int64 {
        return 0
    }
}

main() {
    return -1
}
