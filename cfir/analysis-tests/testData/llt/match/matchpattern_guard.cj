


main(): Int64 {
    print(case01(<!CANNOT_CONVERT_LITERAL!>1<!>).toString())
    case02(<!CANNOT_CONVERT_LITERAL!>1<!>)
    return 0
}

func case01(x: Float32) {
    let a: Float32 = <!CANNOT_CONVERT_LITERAL!>1<!>
    let b: Float32 = <!CANNOT_CONVERT_LITERAL!>0<!>
    var result: Int64 = match (<!NON_EXHAUSTIVE_MATCH!>Year(x)<!>) {
        case _ where false => 1
    }
    return result
}

func case02(x: Float32) {
    let a: Float32 = <!CANNOT_CONVERT_LITERAL!>1<!>
    let b: Float32 = <!CANNOT_CONVERT_LITERAL!>0<!>
    var result: Int64 = match (Year(x)) {
        case _ where x <!INVALID_BINARY_OPERATOR!>!=<!> 1 => 1
    }
    return result
}

enum TimeUnit {
    Year(Float32)
}
