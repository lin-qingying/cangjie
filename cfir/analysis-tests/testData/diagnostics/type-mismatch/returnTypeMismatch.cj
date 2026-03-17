func badReturn(): Int64 {
    return <!RETURN_TYPE_MISMATCH!>true<!>
}
