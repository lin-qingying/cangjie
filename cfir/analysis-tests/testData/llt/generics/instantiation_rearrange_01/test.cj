// FILE: pkg.cj


package pkg

public interface I0<T> {
    static func test(a: Int64): Unit
}

public interface I<T> <: I0<T> {
    static func test(a: Int64): Unit
}

extend Int64 <: I<Int64> {
    public static func test(a: Int64): Unit {
        println("Int64")
    }
}

public class A<T> {}

extend<T> A<T> <: I<A<T>> where T <: I<T> {
    public static func test(a: Int64): Unit {
        cj<T>(a)
        println("A")
    }
}

public func cj<T>(v: Int64): Unit where T <: I0<T> {
    return T.test(v)
}

extend<T> Option<T> <: I<Option<T>> where T <: I<T> {
    public static func test(a: Int64): Unit {
        T.test(a)
    }
}

// FILE: test.cj



internal import pkg.*

main() {
    A<Int64>.test(0)
    0
}
