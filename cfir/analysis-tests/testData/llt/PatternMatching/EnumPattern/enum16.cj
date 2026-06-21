

enum A<T> {
    A1(T)
}

enum E {
    | E0
    | E1(Int64, (<!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>A<!>, Bool))
}

main() {
    let x = E0
    <!NON_EXHAUSTIVE_MATCH!>match (x) {
        case E0 => 0
    }<!>
}
