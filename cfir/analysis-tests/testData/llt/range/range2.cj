


main() {
    var range1 = 1..Int64(10) : 3
    var range2 = 1..10 : <!TYPE_MISMATCH!>true<!>
    var range3 = 1.0..10.0 : <!TYPE_MISMATCH!>2.0<!>
    var range4 = 1..10 : <!RANGE_STEP_CANNOT_BE_ZERO!>0<!>
    return 0
}
