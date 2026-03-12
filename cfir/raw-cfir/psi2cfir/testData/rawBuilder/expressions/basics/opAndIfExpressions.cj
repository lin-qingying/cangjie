func opAndIfExpressions(a: Int64, b: Int64, c: Int64): Int64 {
    let d = -+-2 + -(2 + a)
    let e = if (a > b) { c } else { b }
    return d + e
}