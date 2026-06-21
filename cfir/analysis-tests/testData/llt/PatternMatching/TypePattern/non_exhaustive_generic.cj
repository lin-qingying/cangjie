

func f<T>(x: T) {
    <!NON_EXHAUSTIVE_MATCH!>match (x) {
        case _: Int64 => 0 // reachable
    }<!>
}

main() {
    f(0)
}
