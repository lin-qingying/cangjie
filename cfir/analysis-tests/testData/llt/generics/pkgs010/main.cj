// FILE: pkg.cj


package pkg

public open class A<T> {
    public static let a = 1
}

func test() {
    A<Int64>()
}

public func foo<T>() {
    A<Int64>()
}

// FILE: main.cj



internal import pkg.*

main() {
    foo<Int64>()
    return
}
