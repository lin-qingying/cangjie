package test

func optionalValue(): ?Int64 {
    return Some(1)
}

func matchBinding(): Bool {
    let opt = optionalValue()
    match (opt) {
        case Some(value) =>
            if (value == 1) {
                return true
            }
        case None => ()
    }
    return false
}
