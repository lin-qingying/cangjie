


main(): Int64 {
    func f() {
        var x = 1
        func g() {
            x
        }
        <!USE_FUNC_CAPTURE_VAR_ALONE!>g<!>
    }
    {
        x: Int64, y: Int64 =>
        var a = x + y
        <!USE_FUNC_CAPTURE_VAR_ALONE!>{=> 2 * a}<!>
    }
    return 0
}
