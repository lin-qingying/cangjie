enum TimeUnit {
    Year(Int32)
    | Month(Int32, Int32)
    | Day(Int32, Int32, Int32)
}

main(): Int64 {
    var time = Day(1, 2, 3)
    var d = match (time) {
        case Year(a) => Int64(a)
        case Month(a, b) => Int64(a + b)
        case Day(a, b, c) => Int64(a + b + c)
    }
    return d
}
