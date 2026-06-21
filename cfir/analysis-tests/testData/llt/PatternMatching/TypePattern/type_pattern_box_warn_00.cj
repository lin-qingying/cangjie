


interface I {}

struct A <: I {}

main() {
    let x = (1, A())
    match (x) {
        case <!UNREACHABLE_PATTERN!>_: (Int64, I)<!> => 0
        case _ => 1
    }
}
