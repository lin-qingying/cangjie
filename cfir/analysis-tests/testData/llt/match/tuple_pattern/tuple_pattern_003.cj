

main(): Int64 {
    let scoreTuple = ("Allen", 90)
    var scoreResult: String = match (scoreTuple) {
        case ("Bob", 90) => "Bob got 90"
        case <!TUPLE_PATTERN_WITH_CORRECT_SIZE_EXPECTED!>(_, _, _)<!> => "someone got 100"
    }
    0
}
