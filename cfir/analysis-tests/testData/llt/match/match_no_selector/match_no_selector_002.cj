


main(): Int64 {
    var b: Int64 = <!MATCH_CASE_MUST_HAVE_DEFAULT!>m<!>atch {
        case false => 0
    }
    return b
}
