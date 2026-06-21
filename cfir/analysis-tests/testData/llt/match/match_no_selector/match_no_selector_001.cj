


func rtTrue() {
    return true
}

main(): Int64 {
    var res = match {
        case false && false => 1
        case rtTrue() => 0
        case 1 > 2 => 3
        case 1 > 2 && false => 4
        case _ => 5
    }
    return res
}
