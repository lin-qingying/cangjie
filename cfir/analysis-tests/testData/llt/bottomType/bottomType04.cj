


main(): Int64 {
    while ({
            =>
            <!INVALID_LOOP_CONTROL!>break<!>;
            false
        }()) {
    }
    return 0;
}
