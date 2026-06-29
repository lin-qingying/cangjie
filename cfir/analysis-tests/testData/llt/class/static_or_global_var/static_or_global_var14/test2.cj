// FILE: a.cj


package pkga

public func foo(lam: () -> Unit): Unit {
    lam()
    lam()
}

// FILE: test2.cj



import pkga.foo

class B {
    public static let <!TYPE_UNINITIALIZED_STATIC_FIELD!>CTX<!>: Int64

    static init() {
        foo(
            {
                =>
                    CTX = 10
                    println(CTX)
            }
        )
    }
}

main() {}
