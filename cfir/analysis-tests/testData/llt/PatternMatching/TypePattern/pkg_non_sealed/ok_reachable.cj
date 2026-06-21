// FILE: pkg.cj


package pkg

public interface I {}

public open class A {}

// FILE: ok_reachable.cj



internal import pkg.*

class B <: A & I {}

func f(x: I) {
    match (x) {
        case _: A => 0
        case _: I => 1
    }
}

main() {
    f(B())
}
