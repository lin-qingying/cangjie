


var r1 = match (((), ())) {
    case (b: Int64, a: Int64) => 2
    case (b: Unit, a: Unit) => 0
    case _ => 1
}

main(): Int64 {
    if (r1 != 0) {
        return 1
    }
    return 0
}
