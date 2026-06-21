

func r(a: Future<Unit>): Bool {
    r(<!TYPE_MISMATCH!>spawn {
        =>
        if (true) {
            return false
        };
        true
    }<!>)
    return true
}

main() {
    0
}
