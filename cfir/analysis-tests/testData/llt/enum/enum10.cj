


enum TimeUnit {
    Year(Int32) | Month(Int32)
}

enum TimeUnit1<T> {
    Year(T)
}

func codegenUnitTest(): Int32 {
    var time1 = <!AMBIGUOUS_CONSTRUCTOR_CALL!>Year<!>(1)
    var time2 = TimeUnit.Month(<!CANNOT_CONVERT_LITERAL!>r'a'<!>)
    return 0
}
