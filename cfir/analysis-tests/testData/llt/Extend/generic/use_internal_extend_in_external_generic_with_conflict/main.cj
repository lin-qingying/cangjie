// FILE: pkg1/pkg1.cj


package pkg1

public class A {}

// FILE: pkg2/pkg2.cj


package pkg2

internal import pkg1.*

interface I1 {
    func foo(): Int64
}

extend A <: I1 {
    public func foo() {
        1
    }
}

public func f<T>() {
    A().foo()
}

// FILE: main.cj


internal import pkg1.*
internal import pkg2.*

interface I2 {
    func foo(): Int64
}

extend A <: I2 {
    public func foo() {
        2
    }
}

main() {
    return f<Int64>() - 1
}
