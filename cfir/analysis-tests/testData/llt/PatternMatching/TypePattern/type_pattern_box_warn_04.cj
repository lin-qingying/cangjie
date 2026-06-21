


interface I {}

class A <: I {}

func f(x: I) {}

main() {
    match (f) {
        case _: (A) -> Unit => 0
        case <!UNREACHABLE_PATTERN!>_<!> => 1
    }
}
