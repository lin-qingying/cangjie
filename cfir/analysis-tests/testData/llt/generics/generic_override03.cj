


interface I1 {
    func foo(): Int64
}

interface I3<T> {
    public func goo(): T
}

class CA<T> <: I3<I1> where T <: I1 {
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

main() {}
