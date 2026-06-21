// FILE: pkg.cj


package pkg

enum B {
    B1

    public func test(a: Int64) {
        a
    }
}

public enum A<T> {
    A1

    public func test(a: Int64) {
        return B1.test(a)
    }
}

public func test() {
    if (A1<Int64>.test(123) != 123) {
        return 1
    }
    return 0
}

// FILE: main.cj



import pkg.*

main() {
    // First part test for using instantiated decls that already instantiated in imported pkg.
    if (A1<Int64>.test(123) != 123) {
        return 1
    }
    if (test() != 0) {
        return 4
    }
    // Second part test for current package instantiation.
    if (A1<Int16>.test(123) != 123) {
        return 1
    }
    return 0
}
