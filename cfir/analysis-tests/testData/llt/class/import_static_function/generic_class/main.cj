// FILE: pkg/pkg.cj


package pkg

public class InteropTest<Z> {
    public static func foo<T>(value: T): Unit {}
}

// FILE: main.cj



internal import pkg.*

public class Integer {
    public init(val: Int32) {}
}

main() {
    InteropTest<Int32>.foo<Integer>(Integer(-123))
    return 0
}
