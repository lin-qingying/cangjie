
func f(): Option<Int64> {
    return <!RETURN_TYPE_MISMATCH!>Int32(4)<!>
}

main(): Int64 {
    f()
    return 0
}
