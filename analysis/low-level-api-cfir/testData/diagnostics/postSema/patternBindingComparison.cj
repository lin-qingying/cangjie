package test

func pair(): (Int64, Int64) {
    return (1, 2)
}

func destructure(): Int64 {
    let (left, right) = pair()
    if (left == 1) {
        return right
    }
    return 0
}
