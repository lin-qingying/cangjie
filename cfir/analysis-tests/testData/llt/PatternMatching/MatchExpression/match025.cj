

main() {
    let scoreTuple = ("Allen", 90)
    var scoreResult: String = match (scoreTuple) {
        case ("Bob", 90) => "Bob got 90"
        case ("Allen", score) => "Allen got ${score}" // matched
        case ("Allen", 100) | ("Bob", 100) => "Allen or Bob got 100"
        case (_, _) => ""
    }
    println(scoreResult)
}
