// FILE: pkg.cj


package pkg

public interface I<T> {}

public func foo<T>(): Unit where T <: I<T> {
}

public struct A {
    public func test<T>(): Unit where T <: I<T> {
        foo<T>()
    }
}

// FILE: main.cj



internal import pkg.*

class B {}

extend B <: I<B> {}

main(): Int64 {
    let a = A()
    a.test<B>()
    0
}
