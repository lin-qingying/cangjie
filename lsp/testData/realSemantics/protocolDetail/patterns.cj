package sample.patterns

func typed(input: Any): Int64 {
    return match (input) {
        case typeBound: Int64 => typeBound + typeBound
        case _ => 0
    }
}

func bind(input: Int64): Int64 {
    return match (input) {
        case bound => bound + bound
    }
}
