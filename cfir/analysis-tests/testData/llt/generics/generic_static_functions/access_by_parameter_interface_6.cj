


interface I {
    static func foo() {
        return 0
    }
}

class D<T> where T <: I {
    public func foo() {
        return T.foo()
    }
}

main() {
    return D<I>().foo()
}
