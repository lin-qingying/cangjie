


main(): Int64 {
    var a = 1
    func f() {
        return a
    }
    func g() {
        return f() + 1
    }
    {a: Int64 => <!USE_FUNC_CAPTURE_VAR_ALONE!>g<!>}
    return 0
}
