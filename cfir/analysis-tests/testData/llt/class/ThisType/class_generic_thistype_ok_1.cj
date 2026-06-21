

class C1<T> {
    public var x: T

    public init(a: T) {
        x = a
    }

    public func foo1(): This {
        return this
    }
}

main() {
    var obj = C1<Int64>(1)
    if (obj.foo1().x == 1) {
        return 0
    }
    return 0
}
