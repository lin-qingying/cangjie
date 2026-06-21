

enum TimeUnit {
    Year(Int32)
    | Month(Int32, Int32)
    | Day(Int32, Int32, Int32)
    | Hour(Int32, Int32, Int32, Int32)
}

main(): Int64 {
    var time = Day(1, 2, 3)
    var d: Int64 = match (time) {
        case Year(a) => <!TYPE_MISMATCH!>a<!>
        case Month(a, <!REDECLARATION!>a<!>) => <!TYPE_MISMATCH!>a + a<!>
        case _ => -1
    }
    if (d == 6) {
        return 0
    }
    return 1
}
