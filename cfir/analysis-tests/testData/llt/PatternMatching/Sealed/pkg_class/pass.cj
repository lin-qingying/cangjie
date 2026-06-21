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

// FILE: pass.cj



internal import pkga.*

abstract sealed class C {
    public open func f() {
        0
    } // no warning
}

func f(x: A) {
    <!NON_EXHAUSTIVE_MATCH!>match (x) {
        case b: B => b.f()
    }<!>
}

main() {
    f(B())
}
