// FILE: testa.cj


package a

public interface IChild {
    func some(): Int64
}

public open class A {}

// FILE: testb.cj


package b

import a.*

public interface I2 {
    func some(): Int64
}

extend A <: I2 {
    public func some() {
        0
    }
}

public interface I1 {}

// FILE: main.cj



import a.*
import <!UNUSED_IMPORT!>b.I1<!>

public class <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>B<!> <: A & IChild {}

main() {}
