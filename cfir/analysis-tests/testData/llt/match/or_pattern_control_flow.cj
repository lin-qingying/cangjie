func trailingBinding(value: Bool): Unit {
    match (value) {
        case true | <!VAR_IN_OR_PATTERN!>binding<!> => ()
    }
}

func leadingBinding(value: Bool): Unit {
    match (value) {
        case <!VAR_IN_OR_PATTERN!>binding<!> | true => ()
    }
}

func trailingTuple(value: (Bool, Int64)): Unit {
    match (value) {
        case (true, _) | (_, _) => ()
    }
}

func onlyTuple(value: (Int64, Int64)): Unit {
    match (value) {
        case (_, _) => ()
    }
}

func enumPayload(value: ?(Int64, Int64)): Unit {
    match (value) {
        case Some((_, _)) | None => ()
    }
}
