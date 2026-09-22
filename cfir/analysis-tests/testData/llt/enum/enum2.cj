


enum TimeUnit {
    Year | Month | Day | Hour
}

func codegenUnitTest(): Int32 {
    var time1 = TimeUnit.Year
    <!INVALID_ENUM_MEMBER_ACCESS!>time1.Year<!>
    time1.<!NO_MATCHING_OPERATOR_INVOKE!>Year<!>()
    return 0
}
