// FILE: pkgs.cj


package pkgs

sealed interface I {
    static func f() {
        1
    }
}

// FILE: fail_2.cj



import pkgs.*

struct <!CANNOT_INHERIT_SEALED!>A<!> <: I {}

main(): Int64 {
    return A.f() - 1
}
