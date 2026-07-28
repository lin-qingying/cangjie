
enum E {
    | A
    | B
}

enum F {
    | A
    | B
}

main() {
    let x = E.A
    match (x) {
        case A => 0
        case F.B => 1
    }
}
