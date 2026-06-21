

main(): Int64 {
    var a: Option<Int64> = 100
    match (a) {
        case _: Option<Option<Int64>> => 1
        case _ => 0
    }
}
