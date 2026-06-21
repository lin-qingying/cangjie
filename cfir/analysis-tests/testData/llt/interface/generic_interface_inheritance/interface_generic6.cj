


interface I<T> {
    func foo(a: T): T
}

class C<T> <: I<T> & <!SUPER_TYPES_DUPLICATE!>I<T><!> & I<Float64> {
    public func foo(a: T): T {
        return a
    }
    public func foo(a: Float64): Float64 {
        return a
    }
}

main() {
    return 0
}
