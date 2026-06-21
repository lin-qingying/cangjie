// FILE: pkg.cj


package pkg

public open class A {
    protected open func ff() {
        1
    }
}

public open class B <: A {
    protected func ff() {
        2
    } // f2, override f1
}

// FILE: main.cj



import pkg.*

extend B {
    func <!EXTEND_MEMBER_CANNOT_SHADOW!>ff<!>() {
        3
    } // f3. conflict 
}

main() {
    let v = B().ff()
    if (v != 3) {
        return 1
    }
    return 0
}
