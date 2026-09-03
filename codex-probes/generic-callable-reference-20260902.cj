class C1 {}

class C2 {}

class A<X> {
    public func foo<Y>(x: X) where Y <: X {
        x
    }
    public func foo<Y>() {}
}

main(): Int64 {
    let a = A<C1>()
    a.foo<C2>(C1())
    let b = a.foo<C2>
    0
}
