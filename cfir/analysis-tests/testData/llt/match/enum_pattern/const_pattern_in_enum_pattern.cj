


enum TimeUnit1 {
    Year(String)
}

main(): Int64 {
    var str = Year("abc")
    var a = match (str) {
        case Year("x") => 1
        case Year("abc") => 0
        case _ => 2
    }
    return a
}
