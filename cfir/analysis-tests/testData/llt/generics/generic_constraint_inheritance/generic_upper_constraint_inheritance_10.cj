


open class C {}

class B <: C {}

extend B {
    public func foo(): Int {
        1
    }
}

interface I {
    func foo(): String
}

func bar<T>(x: T) where <!INHERIT_MEMBER_TYPE_INCONSISTENT!>T <: C & B & I<!> {
    return x.foo()
}

main() {
    return 1
}
