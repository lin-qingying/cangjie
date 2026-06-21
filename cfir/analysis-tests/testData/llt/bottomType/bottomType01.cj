

main(): Int64 {
    let val = {
        =>
        var a: Bool = false
        a
        return 1;
    }
    if (<!TYPE_MISMATCH!>val<!>) {
        return 1
    }
    return 0
}
