// FILE: testa.cj


package a

public class A<T> {}

public class B {}

// FILE: testb.cj


package b

internal import a.*

interface I1 {
    func foo(): Int64
}

extend<T> A<T> <: I1 {
    public func foo() {
        1
    }
}

extend B <: I1 {
    public func foo() {
        1
    }
}

public func funcA<T>() {
    A<T>().foo()
}

public func funcB() {
    B().foo()
}

// FILE: main02.cj


import a
internal import b.*

interface I2 {
    func foo(): Int64
}

extend<T> a.A<T> <: I2 {
    public func foo() {
        2
    }
}

extend a.B <: I2 {
    public func foo() {
        2
    }
}

public func bar() {
    if (a.B().foo() == 2) {
        println("non-generic extending works well")
    }
    let res = funcB()
    if (res == 1) {
        println("non-generic extending works well")
    }

    // Test generic extend in different pkg, but instantiated in the same pkg.
    if (a.A<Int64>().foo() != 2) {
        return 1
    }
    return funcA<Int64>() - 1
}

main() {
    return bar()
}
