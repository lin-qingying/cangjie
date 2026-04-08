package sample.sessions.lambda

func apply(value: Int64, transform: (Int64) -> Int64): Int64 {
    return transform(value)
}

func runLambda(base: Int64): Int64 {
    return apply(base, {input => input + 1})
}
