package sample.stubs

class Greeter {
    func member(): Int64 {
        return 42
    }
}

let PI: Float64 = 3.14
var (left, right) = (1, 2)

func topLevel(): Int64 {
    return 42
}

extend Int64 {
    func clamp(): Int64 {
        return 0
    }
}
