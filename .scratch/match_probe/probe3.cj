package test

func pair(): (Int64, Int64) {
    return (1, 2)
}

func matchBinding(): Bool {
    match (pair()) {
        case (value, _) =>
            if (value == 1) {
                return true
            }
    }
    return false
}
