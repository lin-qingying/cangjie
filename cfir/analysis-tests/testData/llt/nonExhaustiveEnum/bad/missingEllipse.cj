

enum A {
    | B | ...
}

main() {
    <!NON_EXHAUSTIVE_MATCH!>match (B) {
        case B => 0
    }<!>
}
