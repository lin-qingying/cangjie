// RUN_PIPELINE_TILL: FRONTEND
func takesInt(x: Int64): Int64 {
    return x
}

main(): Unit {
    // Supports CFIR_ prefixed diagnostic names from factoryName.
    takesInt(<!CANNOT_CONVERT_LITERAL!>true<!>)
}
