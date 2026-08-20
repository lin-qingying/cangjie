



main(): Int64 {
    var a: Array<String> = ["aa"]
    var z = a.<!AMBIGUOUS_USE!>clone<!>
    a.<!CANNOT_ASSIGN_TO_IMMUTABLE!>size<!> = 1
    var x = a.clone(<!ARGUMENT_TYPE_MISMATCH!>2<!>)
    return 0
}
