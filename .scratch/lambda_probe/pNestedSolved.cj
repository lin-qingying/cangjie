func pNestedSolved(): Unit {
    let f = {i: Int64 =>
        let g = {j => j}
        g(i)}
    f(1)
    return
}