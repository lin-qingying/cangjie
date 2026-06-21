// FILE: pkga.cj


package pkga

sealed interface I {
    func f(): Int64
}

abstract sealed class A {}

extend A <: I {
    public func f() {
        0
    }
}

abstract sealed class B <: A {}

public class C <: B {}

// FILE: fail.cj



internal import pkga.*

class <!CANNOT_INHERIT_SEALED!>C<!> <: I { // error
    public func f() {
        2
    }
}

interface <!CANNOT_INHERIT_SEALED!>J<!> <: I {} // error

class D {}

extend <!CANNOT_INHERIT_SEALED!>D<!> <: I { // error
    public func f() {
        3
    }
}

main() {}
