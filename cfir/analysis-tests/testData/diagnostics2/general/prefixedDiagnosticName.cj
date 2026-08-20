// RUN_PIPELINE_TILL: FRONTEND
func takesInt(x: Int64): Int64 {
    return x
}

main(): Unit {
    takesInt(<!CANNOT_CONVERT_LITERAL!>true<!>)
}
