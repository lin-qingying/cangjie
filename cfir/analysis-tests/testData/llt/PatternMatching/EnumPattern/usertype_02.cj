

enum E {
    | A(Int64)
    | A(Int64, Int64)
}

main() {
    let x = E.A(0)
    match (x) {
        case A(0) => 0
        case E.A(1) => 1
        case A(_) => 2
        case E.A(_, _) => 3
    }
}
