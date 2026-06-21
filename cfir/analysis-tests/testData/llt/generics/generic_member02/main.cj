// FILE: pkg.cj


package pkg

open class B {
    public func test<T>(a: T) {
        a
    }
}

public class A<T> {
    public func test(a: T) {
        return B().test(a)
    }
    public func foo<X>(b: X) {
        b
    }
    public func goo<T>(x: T, y!: T = x): T {
        y
    }
}

public class C {
    public func foo<X>(b: X) {
        b
    }
}

// FILE: main.cj



internal import pkg.*

main() {
    let obj = A<Int64>()
    if (obj.test(123) != 123 || obj.foo(12) != 12) {
        return 1
    }
    if (obj.foo(234) != 234) {
        return 2
    }
    let c = C()
    if (c.foo("23") != "23") {
        return 3
    }
    return 0
}
