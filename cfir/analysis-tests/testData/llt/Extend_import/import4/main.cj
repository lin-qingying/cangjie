// FILE: p1.cj


package p1

public class Foo {}

public class Bar {}

// FILE: p2.cj


package p2

internal import p1.*

interface I1 {
    func f(): Unit
}

public interface I2 {
    func g(): Unit
}

extend Foo <: I1 & I2 { // not public
    public func f(): Unit {}
    public func g(): Unit {}
}

extend Bar <: I2 { // public
    public func g(): Unit {}
}

// FILE: main.cj



internal import p1.*
internal import p2.*

main() {
    let b = Bar()
    b.g()
    return 0
}
