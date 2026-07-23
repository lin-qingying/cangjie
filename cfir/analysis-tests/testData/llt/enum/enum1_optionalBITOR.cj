


enum TimeUnit {
    | Year | Month | Day | Hour
}

enum TimeUnit2 {
    | Year2(Int32)
    | Month2(Int32, Int32)
}

func codegenUnitTest(): TimeUnit {
    var time1 = TimeUnit.<!NOT_MEMBER_OF!>Year3<!>(1)
    var time2 = TimeUnit2.Month2<!WRONG_NUMBER_OF_ARGUMENTS!>(1)<!>
    var time3 = TimeUnit2.<!NOT_MEMBER_OF!>Year<!>
    var time4 = <!NO_MATCHING_OPERATOR_INVOKE!>TimeUnit.Year<!>(1)
    return <!RETURN_TYPE_MISMATCH!>0<!>
}
