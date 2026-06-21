

main(): Int64 {
    var b: Int64 = match {
        case true => 0
        case <!TYPE_MISMATCH!>1<!> => 2
        case _ => 3
    }
    return b
}
