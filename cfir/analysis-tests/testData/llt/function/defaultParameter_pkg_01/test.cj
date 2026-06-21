// FILE: pkg.cj


package pkg

public class A<T> {
    private static let v: Int64 = 12
    public static func test(a!: Int64 = v): Int64 {
        a
    }
}

// FILE: test.cj


internal import pkg.*

main() {
    let v = A<Int64>.test()
    if (v != 12) {
        return 1
    }
    return 0
}
