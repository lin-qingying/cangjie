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

// FILE: main.cj



import p2.B

class C <: B {
    func foo2() {
        foo()
    }
}

main() {
    0
}
