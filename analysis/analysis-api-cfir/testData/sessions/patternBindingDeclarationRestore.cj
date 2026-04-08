package sample.sessions.patterns

func consumePair(pair: (Int64, Int64)): Int64 {
    let (left, right) = pair
    return left + right
}
