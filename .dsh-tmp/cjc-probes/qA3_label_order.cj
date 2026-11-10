main(): Int64 {
    let a: (x: Int64, y: String) = (1, "a")
    let b: (y: String, x: Int64) = a
    let c: (p: Int64, q: String) = b
    return c[0]
}
