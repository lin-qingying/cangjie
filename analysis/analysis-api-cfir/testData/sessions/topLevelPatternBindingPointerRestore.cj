package sample.sessions.patterns

var (topLeft, topRight) = (1, 2)

func useTopLeft(): Int64 {
    return topLeft + topRight
}
