

enum A<T> {
    | B | ...
}

main() {
    let a = A<Int64>.B
    <!NON_EXHAUSTIVE_MATCH!>match (a) {
        case B => 0
    }<!>
}
