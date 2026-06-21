// FILE: a.cj


package pkga

public open class A {
    protected open func foo() {
        return 1
    }

    protected static func foo1() {
        return 1
    }
}

public type TA = A

extend TA {
    public func me() {
        foo() + foo1()
    }
}

// FILE: pass01.cj


package pkg

import pkga.TA

class B <: TA {
    func foo_b() {
        foo() + foo1() + me()
    }
}

main(): Int64 {
    return B().me() + B().foo_b() - 6
}
