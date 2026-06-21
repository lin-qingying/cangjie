

// DIAGNOSTICS: -UNUSED_VARIABLE

enum TimeUnit {
    Year(Int32)
    | Month(Int32, Int32)
    | Day(Int32, Int32, Int32)
    | Hour(Int32, Int32, Int32, Int32)
}

main(): Int64 {
    var time = Day(1, 2, 3)
    var d = match (time) {
        case Year(y) => y
        case Year(<!VAR_IN_OR_PATTERN!>z<!>) | Month(<!VAR_IN_OR_PATTERN!>y<!>, _) => <!UNRESOLVED_REFERENCE!>z<!>
        case _ => -1
    }
    return 0
}
