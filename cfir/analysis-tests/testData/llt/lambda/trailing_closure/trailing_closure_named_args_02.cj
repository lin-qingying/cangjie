


main(): Int64 {
    func g(h!: (Int64) -> Int64, a!: (Int64) -> Int64) {
        h(0) - a(2)
    }
    var t = g(a: {t => t + 1}) <!ARGUMENT_PASSED_TWICE!>{<!>t => t + 1}
    return 0
}
