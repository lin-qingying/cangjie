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

// FILE: pass.cj



internal import pkga.*

interface J {
    func g(): Int64
}

extend B <: J { // ok, sealed class can implement new interface
    public func g() {
        9
    }
}

func f(x: I) {
    <!NON_EXHAUSTIVE_MATCH!>match (x) {
        case c: B => c.g()
        case a: A => a.f()
    }<!>
}

main() {
    f(C())
}
