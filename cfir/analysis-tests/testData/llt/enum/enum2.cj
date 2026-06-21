


enum TimeUnit {
    Year | Month | Day | Hour
}

func codegenUnitTest(): Int32 {
    var time1 = TimeUnit.Year
    time1.<!NOT_MEMBER_OF!>Year<!>
    time1.<!NO_MATCHING_OPERATOR_INVOKE!>Year<!>()
    return 0
}
