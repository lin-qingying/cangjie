main(): Int64 {
    let a: (x: Int64, y: String) = (1, "a")
    let b: (p: Int64, q: String) = a
    let c: (Int64, String) = b
    return c[0]
}
