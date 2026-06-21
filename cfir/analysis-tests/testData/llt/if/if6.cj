

main(): Int64 {
    var a: Int32 = 1;
    let x: Int64 = if (false) {
        2;
    } else {
        <!TYPE_MISMATCH!>a<!>;
    };
    return 0;
}
