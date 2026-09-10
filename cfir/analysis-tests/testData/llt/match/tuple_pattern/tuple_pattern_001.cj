

main(): Int64 {
    let scoreTuple = ("Allen", 90)
    var scoreResult: String = match (scoreTuple) {
        case ("Bob", 90) => "Bob got 90"
        case <!UNREACHABLE_PATTERN!>("Allen", score)<!> => "Allen got ${score}"
        case <!UNREACHABLE_PATTERN!>(_, 100)<!> => "someone got 100"
        case <!UNREACHABLE_PATTERN!>(_, _)<!> => ""
    }
    print(scoreResult)
    0
}
