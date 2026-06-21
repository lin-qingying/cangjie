


const func sign(x: Int64) {
    match {
        case x < 0 => -1
        case x == 0 => 0
        case _ => 1
    }
}

main() {
    sign(0)
}
