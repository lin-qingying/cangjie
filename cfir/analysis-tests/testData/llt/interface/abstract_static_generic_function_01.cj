

interface I<M> {
    static func test<T>(a: T): M
}

class Base <: I<Base> {
    var a = 111
    public static func test<T>(a: T): Base {
        let b = Base()
        b.a = 0
        return b
    }
}

class A {
    static func foo<T>(a: T): T where T <: I<T> {
        var b: T = T.test(a)
        return b
    }
}

main(): Int64 {
    let ins = A.foo<Base>(Base())
    return ins.a
}
