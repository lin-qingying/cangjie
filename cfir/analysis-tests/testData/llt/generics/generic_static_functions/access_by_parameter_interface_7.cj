

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

class C1 <: I {
    public static func foo(a: Int32) {
        return 1
    }
}

main() {
    return D<C1>().foo()
}
