// FILE: pkg.cj


package pkg

enum B {
    B1

    public func test<T>(a: T, b!: A<Int8> = A1): T {
        a
    }
}

public enum A<T> {
    A1

    public func test(a: T) {
        return B1.test(a)
    }
    public func foo<X>(b!: A<T> = A1) {
        b
    }
}

public func test() {
    A1<Int64>.foo<Int64>()
    return 0
}

// FILE: main.cj



import pkg.*

main() {
    A1<Int64>.foo<Int64>()
    if (test() != 0) {
        return 4
    }
    A1<Int16>.foo<Rune>()
    return 0
}
