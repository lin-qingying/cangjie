

interface I {
    static func foo() {
        return 1
    }
}

class D<T> where T <: C1 & I {
    public func foo() {
        return T.foo()
    }
}

class C1 <: I {
    public static func foo() {
        return 0
    }
}

main() {
    return D<C1>().foo()
}
