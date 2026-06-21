// FILE: pkg1.cj


package pkg1

public interface I0 {
    func test(): UInt64
}

public interface I1 <: I0 {
    func test(): UInt64 {
        return 1
    }
}

public class A {}

extend A <: I1 {
    public func test(): UInt64 {
        return 2
    }
}

// FILE: pkg2.cj


package pkg2

import pkg1.*

public interface I2 <: I1 {}

extend A <: I2 {}

// FILE: test.cj



package pkg

import pkg1.*
import pkg2.*

main() {
    var a = A()
    println(a.test())
    return
}
