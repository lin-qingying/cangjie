

main(): Int64 {
    return Int64(Data().foo())
}

abstract class Base {
    private func <!INVALID_MEMBER_VISIBILITY_IN_CLASS!>foo<!>(): Int32
}

class Data <: Base {
    public func foo(): Int32 {
        return 0
    }
}
