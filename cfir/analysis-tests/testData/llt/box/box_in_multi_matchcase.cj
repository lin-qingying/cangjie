

func f(value: Any): Unit {
    match (value) {
        case x: Int8 => match (value) {
            case v: Int8 => Int64(v)
            case _ => throw Exception()
        }
        case _ => throw Exception()
    }
}

main() {
    f(Int8(1))
    0
}
