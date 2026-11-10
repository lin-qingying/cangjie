type NamedTuple = (x: Int64, y: String)

main(): Int64 {
    let t: NamedTuple = (1, "a")
    let u: (Int64, String) = t
    let v: NamedTuple = u
    return t[0] + v[0]
}
