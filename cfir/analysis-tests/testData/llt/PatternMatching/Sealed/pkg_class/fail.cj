// FILE: pkga.cj


package pkga

public interface I {
    func f(): Int64
}

abstract sealed class A {}

extend A <: I {
    public func f() {
        0
    }
}

public class B <: A {}

// FILE: fail.cj



internal import pkga.*

class <!CANNOT_INHERIT_SEALED!>C<!> <: A {} // error

func f(x: A) {
    match (x) {
        case b: B => b.f()
        case a: A => a.f()
    }
}

main() {
    f(B())
}
