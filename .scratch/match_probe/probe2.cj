package test

func pair(): (Int64, Int64) {
    return (1, 2)
}

func matchBinding(): Bool {
    match (pair()) {
        case (value, _) =>
            return value == 1
        case _ =>
            return false
    }
}
