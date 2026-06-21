// FILE: pkg0.cj


package pkg0

public class A {}

// FILE: pkg1.cj


package pkg1

internal import pkg0.*

public interface I {
    func fi(): Int64
}

public interface J {
    func fj(): Int64
}

extend A <: I & J {
    public func fi() {
        1
    }
    public func fj() {
        2
    }
}

// FILE: pkg2.cj



package pkg2

internal import pkg0.A
internal import pkg1.I

public interface K {}

extend A <: K {
    public func fk() {
        this.fj() + 1
    } // pkg1.J not import, cannot access 'fj'.
}
