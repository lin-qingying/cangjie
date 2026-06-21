// FILE: pkg/pkg.cj


package pkg

interface I {
    func h1(): Int64
    func h2() {
        h1()
    }
}

struct A<T> <: I {
    public func h1() {
        2
    }
}

public func foo<T>(v: T) {
    let a = A<T>()
    a.h2()
}

// FILE: main.cj


internal import pkg.*

main(): Int64 {
    return foo<Rune>(r'a') - 2
}
