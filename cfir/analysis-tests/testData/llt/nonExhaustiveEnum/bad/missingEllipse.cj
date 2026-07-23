

enum A {
    | B | ...
}

main() {
    match (<!NON_EXHAUSTIVE_MATCH!>B<!>) {
        case B => 0
    }
}
