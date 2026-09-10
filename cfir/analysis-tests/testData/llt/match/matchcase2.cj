

func foo(): Int32 {
    var a: Int32 = 1
    var m: Int32 = 2
    var b = match (m) {
        case <!CANNOT_CONVERT_LITERAL!>1.1<!> => 1
        case 2 => 2
        case _ => 3
    }
    return b
}
