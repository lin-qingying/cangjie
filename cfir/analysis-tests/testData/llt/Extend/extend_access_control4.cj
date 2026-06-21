

open class A {
    protected var v2 = 1
    protected func g(): Int64 {
        return 10
    }
}

extend A {
    public func foo(): Int64 {
        return v2 + g()
    }
}

class B <: A {}

extend B {
    public func goo(): Int64 {
        return v2 + g()
    }
}

main(): Int64 {
    let a = A()
    let b = B()
    return a.foo() + b.goo() - 22
}
