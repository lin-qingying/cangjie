

// All branch of match should be Option<ToString>
func foo(): Option<ToString> {
    let sub: ToString = 1
    match (1) {
        case 2 => sub
        case _ => None<ToString>
    }
}

main() {
    let x = foo()
    match (x) {
        case Some(x) => return 1
        case None => return 0
    }
}
