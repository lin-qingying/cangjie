

main(): Int64 {
    var a: Int32 = 1

    func foo() {
        return a
    }

    var ret = if (true) {
        <!USE_FUNC_CAPTURE_VAR_ALONE!>foo<!>
    } else {
        <!USE_FUNC_CAPTURE_VAR_ALONE!>foo<!>
    }

    if (ret() != 1) {
        return 1
    }

    return 0
}
