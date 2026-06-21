// FILE: pkg.cj


package pkg

sealed interface I {}

public class A <: I {}

class B <: I {}

// FILE: pass.cj



internal import pkg.*

func f(x: I) {
    match (x) {
        case _: A => 0
        case _ => 1
    }
}

main() {
    f(A())
}
