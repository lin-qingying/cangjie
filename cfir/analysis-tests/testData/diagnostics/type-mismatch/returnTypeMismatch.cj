// RUN_PIPELINE_TILL: FRONTEND
func badReturn(): Int64 {
    return <!CANNOT_CONVERT_LITERAL!>true<!>
}
