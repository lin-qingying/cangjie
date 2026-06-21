

main(): Int64 {
    func f(a: Int64) {
        func g(h: (Int64) -> Int64) {
            h(0) - a
        }
        g
    }
    var t = <!TOO_MANY_ARGUMENTS!>f(1) {t => t + 1}<!>
    return 0
}
