

main(): Int8 {
    var a: Int8 = 0;
    match {
        case _ => a
    } >> (match {
        case _ => (1, a)[1]
    } & 7)
}
