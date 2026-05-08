// RUN_PIPELINE_TILL: FRONTEND
main(): Unit {
    var n: Int64 = 1
    n = <!ASSIGNMENT_TYPE_MISMATCH!>true<!>
}
