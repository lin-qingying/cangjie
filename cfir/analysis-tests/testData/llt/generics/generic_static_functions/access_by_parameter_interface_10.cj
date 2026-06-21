


class E1 {}

class E2 {}

interface I {
    static func foo(): E1 {
        return E1()
    }
}

class D<T> where <!INHERIT_MEMBER_TYPE_INCONSISTENT!>T <: C1 & I<!> {
    public func foo() {
        return T.foo()
    }
}

class C1 <: I {
    public static func <!RETURN_TYPE_INCOMPATIBLE!>foo<!>(): E2 {
        return E2()
    }
}

main() {
    let obj = D<C1>().foo()
    return 1
}
