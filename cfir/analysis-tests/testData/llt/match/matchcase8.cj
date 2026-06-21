

func foo(): Int32 {
    let x: Int64 = 1
    let flag: Bool = true
    var b: Int32 = match (<!NON_EXHAUSTIVE_MATCH!>x<!>) {
        case _ where flag => 1
        case _ where false => 2
    }
    return b
}
