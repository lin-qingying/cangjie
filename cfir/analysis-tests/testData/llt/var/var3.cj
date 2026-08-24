main(): Int64 {
    let val = { =>
    var a:Bool = false
        a
        return 1;
    }
    if (<!INVALID_UNARY_EXPR!>!val<!>) {
        return 1
    }
    return 0
}