// FILE: pkg/pkg.cj


package pkg

public open class C {}

extend C {
    protected func f() {
        1
    }
    protected mut prop p: Int64 {
        get() {
            0
        }
        set(v) {}
    }
}

// FILE: main.cj



internal import pkg.*

main() {
    let a = C()
    a.<!NO_MATCH_FUNCTION_DECLARATION_FOR_CALL!>f<!>()
    <!INVALID_ACCESS_CONTROL!>a<!>.p = 0
    <!INVALID_ACCESS_CONTROL!>a<!>.p
    return 0
}
