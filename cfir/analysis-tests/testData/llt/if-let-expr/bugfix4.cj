public func foo(): Bool {
    <!TYPE_MISMATCH!>if (let a <- 0) {
    }<!> else <!TYPE_MISMATCH!>{
    }<!>
}

main () {
    foo()
    0
}

