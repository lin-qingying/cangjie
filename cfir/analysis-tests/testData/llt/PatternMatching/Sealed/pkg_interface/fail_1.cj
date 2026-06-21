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

// FILE: fail_1.cj



internal import pkga.*

enum <!CANNOT_INHERIT_SEALED!>Color<!> <: I { // error
    RED | GREEN | BLUE

    public func f() {
        2
    }
}

main() {}
