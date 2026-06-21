


main(): Int64 {
    let a = 1
    func f(x: Int64, f: (Int64) -> Int64) {
        return f(x)
    }
    func g(x: Int64) {
        return x ^ 2
    }
    var lambda = {b: Int64, g: (Int64) -> Int64 => g(b)}
    f(1, {t => t * a})
    lambda(2, {t => t * a})
    lambda(3, g)
    return 0
}
