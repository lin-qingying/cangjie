// FILE: p1.cj


package p1

public open class A {}

extend A {
    protected func foo(): Int64 {
        1
    }
}

// FILE: p2.cj


package p2

import p1.A

public open class B <: A {}

// FILE: main_err.cj



import p2.B

class C <: B {
    func foo2() {
        <!AMBIGUOUS_FUNCTION_CALL!>foo<!>()
    }
    func <!EXTEND_MEMBER_CANNOT_SHADOW!>foo<!>() {
        2
    }
}

main() {
    0
}
