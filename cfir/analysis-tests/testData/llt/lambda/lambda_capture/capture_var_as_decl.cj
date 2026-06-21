


main(): Int64 {
    var a = 1
    func f(): Int64 {
        return a
    }
    var lambda = <!USE_FUNC_CAPTURE_VAR_ALONE!>{y: Int64 => y + a}<!>
    var g = <!USE_FUNC_CAPTURE_VAR_ALONE!>f<!>
    return 0
}
