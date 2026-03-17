func takesInt(x: Int64): Int64 {
    return x
}

main() {
    // Argument mapping error
    takesInt(<!ARGUMENT_TYPE_MISMATCH!>true<!>)
}
