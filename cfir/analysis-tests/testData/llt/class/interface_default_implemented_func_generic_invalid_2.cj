


interface I1<T> {
    func foo1(a: T): Int64 {
        return 0
    }
}

struct R1<T> <: I1<T> {}

extend<T> R1<T> {
    public func <!EXTEND_MEMBER_CANNOT_SHADOW!>foo1<!>(a: T): Int64 {
        return 0
    }
}

main() {
    return -1
}
