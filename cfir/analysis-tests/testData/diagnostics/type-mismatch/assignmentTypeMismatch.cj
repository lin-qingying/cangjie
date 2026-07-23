// RUN_PIPELINE_TILL: FRONTEND
main(): Unit {
    var n: Int64 = 1
    n = <!CANNOT_CONVERT_LITERAL!>true<!>
}
