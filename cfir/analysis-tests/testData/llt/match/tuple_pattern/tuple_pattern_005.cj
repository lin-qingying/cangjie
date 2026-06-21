

main(): Int64 {
    let scoreTuple = ("Allen", 90)
    var scoreResult: String = match (scoreTuple) {
        case (a, <!REDECLARATION!>a<!>) => "Bob got 90"
        case (_, 100) => "someone got 100"
    }
    0
}
