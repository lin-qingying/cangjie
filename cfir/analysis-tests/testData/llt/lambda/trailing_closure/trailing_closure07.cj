


main(): Int64 {
    func f() {
        func g(h: (Int64) -> Int64) { // label 1
            h(0)
        }
        g
    }
    func f(h: (Int64) -> Int64) { // label 2
        h(1)
    }
    var res = f() {t => t + 1} // match label 2
    return res - 2
}
