


open class C {}

extend C {
    public func foo(): Int {
        1
    }
}

interface I {
    func foo(): String
}

func bar<T>(x: T) where <!INHERIT_MEMBER_TYPE_INCONSISTENT!>T <: C & I<!> {
    return x.foo()
}

main() {
    return 1
}
