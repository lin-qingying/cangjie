func pNestedUnsolved(): Unit {
    let f = {i: Int64 =>
        let g = {j => j}
        let _ = g}
    f(1)
    return
}