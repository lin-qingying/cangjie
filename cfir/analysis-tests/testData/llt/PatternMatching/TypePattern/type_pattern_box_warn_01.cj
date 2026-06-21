


interface I {}

struct A <: I {}

func f(x: I) {}

main() {
    match (f) {
        case <!UNREACHABLE_PATTERN!>_: (A) -> Unit<!> => 0
        case _ => 1
    }
}
