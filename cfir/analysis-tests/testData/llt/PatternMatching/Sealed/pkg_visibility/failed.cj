// FILE: pkg.cj


package pkg

sealed interface I {}

public class A <: I {}

class B <: I {}

// FILE: failed.cj



internal import pkg.*

func f(x: I) {
    <!NON_EXHAUSTIVE_MATCH!>match (x) {
        case _: A => 0
        case _: <!UNRESOLVED_REFERENCE!>B<!> => 1
    }<!>
}

main() {
    f(A())
}
