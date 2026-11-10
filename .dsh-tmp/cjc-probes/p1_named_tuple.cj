typealias NamedTuple = (x: Int64, y: String)

main(): Int64 {
    let t: NamedTuple = (1, "a")
    return t[0]
}
