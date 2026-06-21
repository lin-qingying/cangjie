


// Both branch of if should be Option<ToString>
func foo(): Option<ToString> {
    let sub: ToString = 1
    if (false) {
        sub
    } else {
        None<ToString>
    }
}

main() {
    let x = foo()
    match (x) {
        case Some(x) => return 1
        case None => return 0
    }
}
