


enum TimeUnit1 {
    Year(String)
}

main(): Int64 {
    let yearTuple = ((Year("Allen"), "Allen"), (Year("def"), 80))
    let allen = "Allen"
    let num80 = 80
    return match (yearTuple) {
        case ((Year("Allen"), "Allen"), (Year("def"), 80)) => 0
        case (_, _) => 1
    }
}
