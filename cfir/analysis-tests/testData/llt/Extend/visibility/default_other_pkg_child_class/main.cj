// FILE: pkg/pkg.cj


package pkg

public open class C {}

extend C {
    func f() {
        1
    }
    private func g() {
        1
    }
    mut prop p: Int64 {
        get() {
            0
        }
        set(v) {}
    }
    private mut prop q: Int64 {
        get() {
            0
        }
        set(v) {}
    }
}

// FILE: main.cj



internal import pkg.*

class C1 <: C {
    func h() {
        <!NO_MATCH_FUNCTION_DECLARATION_FOR_CALL!>f<!>()
        <!UNRESOLVED_REFERENCE!>g<!>()
        <!INVALID_ACCESS_CONTROL!>p<!> = 0
        <!INVALID_ACCESS_CONTROL!>p<!>
        <!UNRESOLVED_REFERENCE!>q<!> = 0
        <!UNRESOLVED_REFERENCE!>q<!>
        0
    }
}

main() {
    0
}
