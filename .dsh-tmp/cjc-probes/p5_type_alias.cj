type NamedTuple = (x: Int64, y: String)
type MixedTuple = (Int64, x: String)
type Callback = (Int64) -> Unit

main(): Int64 {
    let t: NamedTuple = (1, "a")
    return t[0]
}
