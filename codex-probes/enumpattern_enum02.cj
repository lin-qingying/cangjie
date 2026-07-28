
enum TimeUnit {
    Year(Int32)
    | Month(Int32, Int32)
}

main(): Int64 {
    var time = Year(1)
    var b = match (time) {
        case Month(1, 2) | Year(1) => 0
        case Year(1) => 1
        case _ => -1
    }
    return b
}
