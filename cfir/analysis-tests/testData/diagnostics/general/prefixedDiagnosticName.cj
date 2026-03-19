// RUN_PIPELINE_TILL: FRONTEND
func takesInt(x: Int64): Int64 {
    return x
}

main() {
    // Supports CFIR_ prefixed diagnostic names from factoryName.
    takesInt(<!ARGUMENT_TYPE_MISMATCH!>true<!>)
}

