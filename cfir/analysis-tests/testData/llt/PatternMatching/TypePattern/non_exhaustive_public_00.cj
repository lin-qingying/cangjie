

public interface I {}

class A <: I {}

class B <: I {}

func f(x: I) {
    <!NON_EXHAUSTIVE_MATCH!>match (x) {
        case _: A => 0
        case _: B => 1
    }<!>
}

main() {
    f(A())
}
