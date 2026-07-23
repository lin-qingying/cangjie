

main(): Int64 {
    func f() {
        func g(h: (Int64) -> Int64) {
            h(0)
        }
        g
    }
    f<!WRONG_NUMBER_OF_ARGUMENTS!>() {t => t + 1}<!>
    return 0
}
