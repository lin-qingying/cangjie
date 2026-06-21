

interface I1 {
    func foo(): Int64
}

interface I3 {
    public func goo(): I1
}

class CA<T> <: I3 where T <: I1 {
    var x: T
    init(_x: T) {
        x = _x
    }
    public func goo(): I1 {
        println("here")
        x
    }
}

struct TEST_1 <: I1 {
    public func foo(): Int64 {
        0
    }
}

main() {
    CA<TEST_1>(TEST_1()).goo()
    0
}
