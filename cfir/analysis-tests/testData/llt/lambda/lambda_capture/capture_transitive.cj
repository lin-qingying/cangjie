


main(): Int64 {
    var a = 1
    func f() {
        return a
    }
    func g() {
        return f() + 1
    }
    {a: Int64 => <!FUNC_CAPTURE_VAR_CANNOT_EXPR!>g<!>}
    return 0
}
