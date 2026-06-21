

interface I<T> {
    func foo(): Int64 {
        return 1
    }
}

class B<T> <: I<String> {}

extend<T> B<T> <: I<Int64> {
    public func <!EXTEND_MEMBER_CANNOT_SHADOW!>foo<!>(): Int64 {
        return 2
    }
}

main(): Int64 {
    return 0
}
