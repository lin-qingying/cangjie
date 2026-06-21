

main(): Int64 {
    let scoreTuple = ("Allen", 90)
    var scoreResult: String = match (scoreTuple) {
        case ("Bob", 90) => "Bob got 90"
        case ("Allen", score) => "Allen got ${score}"
        case (_, 100) => "someone got 100"
        case (_, _) => ""
    }
    print(scoreResult)
    0
}
