

public func foo(): Bool {
    <!TYPE_MISMATCH!>while (let a <- 0) {
    }<!>
}

main() {
    foo()
    0
}
