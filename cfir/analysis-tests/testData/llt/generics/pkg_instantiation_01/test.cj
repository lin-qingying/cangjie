// FILE: pkg.cj


package pkg

public class A<T> {
    public static var a: Int64 = 1
}

public func test() {
    A<Int64>.a = 3
}

// FILE: test.cj



internal import pkg.*

main() {
    if (A<Int64>.a != 1) {
        return 1
    }
    test()
    if (A<Int64>.a != 3) {
        return 2
    }
    return 0
}
