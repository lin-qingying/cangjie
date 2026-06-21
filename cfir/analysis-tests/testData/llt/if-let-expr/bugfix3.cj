public func foo(): Bool {
    <!TYPE_MISMATCH!>if (let a <- 0) {
    }<!>
}

main () {
    foo()
    0
}


