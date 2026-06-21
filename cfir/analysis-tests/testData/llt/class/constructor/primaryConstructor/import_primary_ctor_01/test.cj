// FILE: pkg.cj


package pkg

class A<T> {
    A(var a: T) {}
}

public func test() {
    let a = A(3)
    return a.a
}

// FILE: test.cj



internal import pkg.*

main() {
    let x = test()
    if (x != 3) {
        return 1
    }
    return 0
}
