

main(): Int64 {
    let t1 = (1, 2);
    if (t1 <!INVALID_BINARY_OPERATOR!>!=<!> (1, 2, 3)) {
        return 0;
    }
    return 1;
}
