

func foo(): Int32 {
    var a: Int32 = 1
    var m: Int32 = 4
    var b = match (<!NON_EXHAUSTIVE_MATCH!>m<!>) {
        case 1 => 1
        case 2 => 2
        case 3 => 3
    }
    return b
}
