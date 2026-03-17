func takesInt(x: Int64): Int64 {
    return x
}

main() {
    // Supports CFIR_ prefixed diagnostic names from factoryName.
    takesInt(<!CFIR_ARGUMENT_TYPE_MISMATCH!>true<!>)
}
