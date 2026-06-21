

enum TimeUnit {
    Year(Int32)
    | Month(Int32, Int32)
    | Day(Int32, Int32, Int32)
    | Hour(Int32, Int32, Int32, Int32)
    | Twounit(TimeUnit, TimeUnit)
}

main(): Int64 {
    var year = Year(1)
    var month = Month(2, 3)
    var day = Day(1, 2, 3)
    var time = Twounit(year, month)
    var d = match (time) {
        case Twounit(Day(1, 2, 3), Month(2, 3)) => 1
        case Twounit(Year(1), Month(2, 3)) => 0
        case _ => -1
    }
    return d
}
