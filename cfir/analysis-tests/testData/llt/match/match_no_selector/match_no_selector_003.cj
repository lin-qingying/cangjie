


main(): Int64 {
    var b: Int64 = match {
        case true => 0
        case <!CANNOT_CONVERT_LITERAL!>1<!> => 2
        case _ => 3
    }
    return b
}
