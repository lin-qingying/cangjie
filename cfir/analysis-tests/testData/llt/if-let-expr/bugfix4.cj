public func foo(): Bool {
    if (let a <- 0) <!TYPE_MISMATCH!>{
    }<!> else <!TYPE_MISMATCH!>{
    }<!>
}

main () {
    foo()
    0
}

