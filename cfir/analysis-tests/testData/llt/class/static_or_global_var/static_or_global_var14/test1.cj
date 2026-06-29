// FILE: a.cj


package pkga

public func foo(lam: () -> Unit): Unit {
    lam()
    lam()
}

// FILE: test1.cj



import pkga.foo

class B {
    public static let CTX: Int64

    static init() {
        CTX = 1
        foo(
            {
                =>
                    <!CANNOT_ASSIGN_TO_IMMUTABLE!>CTX = 10<!>
                    println(CTX)
            }
        )
    }
}

main() {}
