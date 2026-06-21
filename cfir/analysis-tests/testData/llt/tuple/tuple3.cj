

main(): Int64 {
    var a: Int64 = 1
    var b = <!INVALID_SUBSCRIPT_EXPR!>a[0]<!>
    return a
}
