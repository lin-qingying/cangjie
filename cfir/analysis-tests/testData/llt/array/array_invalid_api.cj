



main(): Int64 {
    var a: Array<String> = ["aa"]
    var z = <!AMBIGUOUS_USE!>a.clone<!>
    a.<!CANNOT_ASSIGN_TO_IMMUTABLE!>size<!> = 1
    var x = a.clone(<!TYPE_MISMATCH!>2<!>)
    return 0
}
