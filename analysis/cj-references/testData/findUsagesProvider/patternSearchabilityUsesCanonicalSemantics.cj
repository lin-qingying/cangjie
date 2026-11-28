package sample.patterns

enum Signal {
    Ready | Waiting
}

func typed(input: Any): Int64 {
    return match (input) {
        case <caret_typedBinder>typeBound: Int64 => typeBound + typeBound
        case _ => 0
    }
}

func bind(input: Int64): Int64 {
    return match (input) {
        case <caret_bareBinder>bound => bound + bound
    }
}

func constructor(input: Signal): Int64 {
    return match (input) {
        case <caret_constructor>Ready => 1
        case _ => 0
    }
}

func wildcard(input: Any): Int64 {
    return match (input) {
        case <caret_wildcard>_: Int64 => 1
        case _ => 0
    }
}
