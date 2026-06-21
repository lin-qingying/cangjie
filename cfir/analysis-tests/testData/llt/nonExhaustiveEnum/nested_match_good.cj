

enum E {
    A | B(Int64) | ...
}

enum D {
    D1 | D2(Int64)
}

let k = (D1, B(10))

main() {
    <!NON_EXHAUSTIVE_MATCH!>match (k) {
        case (D1, A) => 1
        case (D1, _) => 2
        case (D2(a), _) => a
    }<!>
}
