

main(): Int64 {
    var a: Int64
    func b() {
        do {
            <!CAPTURE_BEFORE_INITIALIZATION!>a<!> = 1 // captured before initialization
        } while (a == 0)
        return a
    }

    b()
    return 0
}
