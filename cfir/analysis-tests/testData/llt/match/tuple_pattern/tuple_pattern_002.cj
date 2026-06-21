

main(): Int64 {
    let scoreTuple = ("Allen", 90)
    var scoreResult: String = match (<!NON_EXHAUSTIVE_MATCH!>scoreTuple<!>) {
        case ("Bob", 90) => "Bob got 90"
        case (_, 100) => "someone got 100"
    }
    0
}
