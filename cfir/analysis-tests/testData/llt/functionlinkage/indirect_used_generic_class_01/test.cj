// FILE: pkg.cj


package pkg

public abstract class A<T> {
    public func test(): Unit {}
}

public abstract class B <: A<String> {}

// FILE: test.cj



// Test for linkage
internal import pkg.*

class C <: B {}

main() {
    let ins = C()
    return 0
}
