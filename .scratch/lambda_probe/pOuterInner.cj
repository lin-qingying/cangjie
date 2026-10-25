func pOuterDeterminesInner(): Int64 {
    var v: Int64 = 1
    let f = {i: Int64 =>
        let g = {j => j}
        g(i)}
    return f(v)
}