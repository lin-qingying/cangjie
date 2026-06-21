// FILE: pkg1/pkg1.cj


package pkg1

public class A<T> {}

// FILE: pkg2/pkg2.cj


package pkg2

internal import pkg1.*

interface I1 {
    func foo(): Int64
}

extend<T> A<T> <: I1 {
    public func foo() {
        0
    }
}

// FILE: main.cj


internal import pkg1.*
internal import pkg2.*

interface I1 {
    func foo(): Int64
}

extend<T> A<T> <: I1 {
    public func foo() {
        1
    }
}

main() {
    return A<Int64>().foo() - 1
}
