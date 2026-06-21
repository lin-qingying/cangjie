


main(): Int64 {
    func f(g: (Int64, Int64) -> Int64) {
        var a = 1
        var b = 2
        return g(a, b)
    }
    var t = f {x, y => x + 2 * y}
    return t - 5
}
