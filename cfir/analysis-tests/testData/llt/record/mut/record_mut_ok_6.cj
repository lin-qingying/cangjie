

interface I1<T> {
    mut func foo1(x: T): Unit
}

struct R1<T> <: I1<T> {
    public var i: T

    public init(x: T) {
        i = x
    }

    public mut func foo1(x: T): Unit {
        i = x
    }
}

main() {
    var a = R1<Int64>(0)
    var b: I1<Int64> = a
    b.foo1(1)
    if (a.i == 0) {
        return 0
    }
    return 1
}
