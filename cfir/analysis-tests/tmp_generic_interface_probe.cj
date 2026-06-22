interface I<T> {
    func foo(a: T): T
}

class C<T> <: I<T> & I<Int32> & I<Float64> {
    public func foo(a: Int32): Int32 {
        return a
    }

    public func foo(a: T): T {
        return a
    }

    public func foo(a: Float64): Float64 {
        return a
    }
}

main() {
    var c = C<Bool>()
    if (c.foo(1) != 1 || !c.foo(true)) {
        return 1
    }
    return 0
}
