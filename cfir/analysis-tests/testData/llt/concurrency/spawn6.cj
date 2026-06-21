

main(): Int64 {
    let a: Int64 = <!TYPE_MISMATCH!>spawn {}<!>
    let b: Int64 = <!UNRESOLVED_REFERENCE!>spwan<!> {}
    return 0
}
