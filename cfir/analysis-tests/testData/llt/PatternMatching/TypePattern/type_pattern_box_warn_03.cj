


interface I {}

struct A <: I {}

func f(x: (Int64, I)): A {
    A()
}

main() {
    let x = (1, f)
    match (x) {
        case <!UNREACHABLE_PATTERN!>_: (Int64, ((Int64, A)) -> I)<!> => 0
        case _ => 1
    }
}
