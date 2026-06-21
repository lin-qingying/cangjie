// FILE: pkg.cj


package pkg

public func test<T>(a: T, b: T) where T <: Equatable<T> {
    a == b
}

// FILE: main.cj



internal import pkg.*

main() {
    test<Int64>(1, 2)
    return
}
