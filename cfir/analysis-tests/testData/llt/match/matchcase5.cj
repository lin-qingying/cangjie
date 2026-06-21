


func foo(): Int32 {
    var a: Int32 = 1
    var m: Int32 = 2
    var b: Int32 = <!MATCH_CASE_MUST_HAVE_DEFAULT!>m<!>atch {
        case m == 1 => 1
        case m == 2 => 2
        case m == 3 => 3
    }
    return b
}
