// FILE: pkg.cj


package pkg

public interface I {
    func f(): Unit
}

class A <: I {
    public func f() {}
}

func test1<T>(a: T) where T <: I {
    a.f()
}

public func test2<T>(a: T) where T <: I {
    test1<T>(a)
}

public func test3<T>() {
    test2<A>(A())
}

// FILE: main.cj



import pkg.*

main() {
    test3<Int64>()
}
