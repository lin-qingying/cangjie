

main(): Int64 {
    var a: Int64 = 0
    var res = match (a) {
        case b: Int64 => b
        case c: Int64 => c + 1
        case _ => -1
    }
    return res
}
