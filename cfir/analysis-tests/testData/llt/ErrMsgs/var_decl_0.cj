


main(): Int64 {
    let x: <!UNDECLARED_TYPE_NAME!>C<!><<!UNDECLARED_TYPE_NAME!>T<!>> = <!NO_MATCH_OPERATOR_FUNCTION_CALL!>1<!>(true)
    let y = <!NO_MATCH_OPERATOR_FUNCTION_CALL!>1<!>(true)
    let z: Bool = <!CANNOT_CONVERT_LITERAL!>1<!>

    let (a1, a2): (<!UNDECLARED_TYPE_NAME!>C<!><<!UNDECLARED_TYPE_NAME!>T<!>1>, <!UNDECLARED_TYPE_NAME!>C<!><<!UNDECLARED_TYPE_NAME!>T<!>2>) = (false, false)
    let <!TUPLE_PATTERN_NOT_MATCH!>(<!>b1, b2) = false
    let (c1, c2): (Int64, Bool) = (<!CANNOT_CONVERT_LITERAL!>t<!>rue, false)

    0
}
