

main() {
    let scoreTuple = ("Allen", 90)
    var scoreResult: String = match (scoreTuple) {
        case ("Bob", 90) => "Bob got 90"
        case <!UNREACHABLE_PATTERN!>("Allen", score)<!> => "Allen got ${score}" // matched
        case <!UNREACHABLE_PATTERN!>("Allen", 100)<!> | <!UNREACHABLE_PATTERN!>("Bob", 100)<!> => "Allen or Bob got 100"
        case <!UNREACHABLE_PATTERN!>(_, _)<!> => ""
    }
    println(scoreResult)
}
