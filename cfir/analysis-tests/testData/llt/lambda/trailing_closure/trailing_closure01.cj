


main(): Int64 {
    func f(a: Int64, b: Int64, g: (Int64, Int64) -> Int64) {
        return g(a, b)
    }
    var t = f(1, 2) {x, y => x + 2 * y}
    return t - 5
}
