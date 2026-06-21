

struct R1<T> {
    public var i: T

    public init(x: T) {
        i = x
    }

    public mut func foo1(): Unit {
    }

    public mut func foo2(x: T) {
        this.i = x
    }
}

main() {
    var obj = R1<Int64>(1)
    obj.foo2(2)
    if (obj.i == 2) {
        return 0
    }
    return 1
}
