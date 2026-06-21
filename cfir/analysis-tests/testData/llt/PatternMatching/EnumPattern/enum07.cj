

enum TimeUnit {
    Year(Int32)
    | Month(Int32, Int32)
    | Day(Int32, Int32, Int32)
    | Hour(Int32, Int32, Int32, Int32)
}

main(): Int64 {
    var time = Day(1, 2, 3)
    var d = match (time) {
        case Year(a) => a
        case Month(a, b) => a + b
        case Day(a, b, c) =>
            print(a.toString() + "," + b.toString() + "," + c.toString())
            a + b + c
        case Hour(1, 2, 3, 4) => Int32(-1)
        case Hour(a, b, c, d) => a + b + c + d
    }
    if (d == 6) {
        return 0
    }
    return 1
}
