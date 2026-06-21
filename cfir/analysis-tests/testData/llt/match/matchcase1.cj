

func foo(): Int32 {
    var a: Int32 = 1
    var m: Int32 = 2
    var b: Int32 = match (m) {
        case 1 => 1
        case 2 => <!TYPE_MISMATCH!>2.1<!>
        case _ => 3
    }
    return b
}
