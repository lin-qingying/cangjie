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
        1
    }
}

public func f() {
    A<Int64>().foo()
}

// FILE: main.cj


internal import pkg1.*
internal import pkg2.*

interface I2 {
    func foo(): Int64
}

extend<T> A<T> <: I2 {
    public func foo() { 2 }
}

main() {
    A<Rune>()
    return f() - 1
}
