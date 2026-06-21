

main(): Int64 {
    func f() {
        func g(h: (Int64) -> Int64) {
            h(0)
        }
        g
    }
    <!TOO_MANY_ARGUMENTS!>f() {t => t + 1}<!>
    return 0
}
