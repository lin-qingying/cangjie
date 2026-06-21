// FILE: pkga.cj


package pkga

public open class A {
    public func f() {
        0
    }
}

// FILE: fail.cj



import pkga.*

class B <: A {
    func <!CANNOT_OVERRIDE!>f<!>() {
        1
    }
}

main() {
    B().f()
}
