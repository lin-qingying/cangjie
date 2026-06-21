// FILE: pkg.cj


package pkg

public interface I<M> {
    static func test<T>(a: T): M
}

public class Base <: I<Base> {
    public var a = 111
    public static func test<T>(a: T): Base {
        let b = Base()
        b.a = 0
        return b
    }
}

public class A {
    public static func foo<T>(a: T): T where T <: I<T> {
        var b: T = T.test(a)
        return b
    }
}

// FILE: main.cj



internal import pkg.*

main(): Int64 {
    let ins = A.foo<Base>(Base())
    return ins.a
}
