// FILE: pkg.cj


package pkg

public enum A<T> {
    A1
}

extend<T> A<T> {
    public func foo<X>(b!: A<T> = A1) {
        return b
    }
}

public func test() {
    A1<Int64>.foo<Int64>()
    return 0
}

// FILE: main.cj



internal import pkg.*

main() {
    if (!(A1<Int64>.foo<Int64>() is A<Int64>)) {
        return 1
    }
    test()
    if (!(A1<Int16>.foo<Rune>() is A<Int16>)) {
        return 2
    }
    return 0
}
