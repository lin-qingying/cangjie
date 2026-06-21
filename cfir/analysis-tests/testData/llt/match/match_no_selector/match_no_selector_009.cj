


func foo(): Option<ToString> {
    let sub: ToString = 1
    match {
        case 2 < 1 => sub
        case _ => None<ToString> // choose this
    }
}

main() {
    let x = foo()
    match (x) {
        case Some(x) => return 1
        case None => return 0
    }
}
