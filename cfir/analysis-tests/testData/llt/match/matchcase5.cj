



func foo(): Int32 {
    var a: Int32 = 1
    var m: Int32 = 2
    var b: Int32 = match {
        case m == 1 => 1
        case m == 2 => 2
        case m == 3 => 3
    }
    return b
}

