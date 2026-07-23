

enum A<T> {
    | B | ...
}

main() {
    let a = A<Int64>.B
    match (<!NON_EXHAUSTIVE_MATCH!>a<!>) {
        case B => 0
    }
}
