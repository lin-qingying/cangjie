public func foo(): Bool {
    <!RETURN_TYPE_MISMATCH!>if (let a <- 0) {
    }<!>
}

main () {
    foo()
    0
}


