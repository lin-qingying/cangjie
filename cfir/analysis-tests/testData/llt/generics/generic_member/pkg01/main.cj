// FILE: pkg.cj


package pkg

public open class Base {}

public class Sub <: Base {}

public interface I1 {
    func test(): Int64 {
        0
    }
}

public open class A {
    public func foo(a: Sub) {
        return 1
    }
}

// FILE: main.cj



internal import pkg.*

public class B <: A {
    public func foo<T>(a: T) {
        return 2
    }
    public func test(): Int64 {
        2
    }
}

extend B <: I1 {}

func test<T>(ins: T) where T <: I1 {
    ins.test()
}

main(): Int64 {
    let a = B()
    if (a.foo(Sub()) != 1) {
        return 1
    }
    if (test(a) != 2) {
        return 2
    }
    return 0
}
