

public interface H {}

sealed interface I <: H {}

class A <: I {}

class B {}

extend B <: I {}

func f(x: I) {
    match (<!NON_EXHAUSTIVE_MATCH!>x<!>) {
        case _: A => 0
        case _: B => 1
        case _: ToString => -1 // unreachable
    }
}

main() {
    f(A())
}
