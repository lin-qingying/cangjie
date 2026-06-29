

public func foo(): Bool {
    <!RETURN_TYPE_MISMATCH!>while (let a <- 0) {
    }<!>
}

main() {
    foo()
    0
}
