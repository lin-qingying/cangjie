// FILE: base.cj
package base
public class Foo {}
public interface R1 {}
public interface R2 {}
public class Both <: R1 & R2 {}

// FILE: one.cj
package one
import base.*
public interface Op1 {
    operator func +(other: R1): R1
}
extend Foo <: Op1 {
    public operator func +(other: R1): R1 { other }
}

// FILE: two.cj
package two
import base.*
public interface Op2 {
    operator func +(other: R2): R2
}
extend Foo <: Op2 {
    public operator func +(other: R2): R2 { other }
}

// FILE: main.cj
import base.*
import <!UNUSED_IMPORT!>one.Op1<!>
import <!UNUSED_IMPORT!>two.Op2<!>
main() {
    let value = Foo() <!INVALID_BINARY_OPERATOR!>+<!> Both()
    return 0
}
