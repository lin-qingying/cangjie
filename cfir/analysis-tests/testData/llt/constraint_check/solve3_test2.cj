


class Z {
    public func test() {}
}

struct R<X> where X <: Z {
    public var i: X
    public init(input: X) {
        input.test()
        i = input
    }
}
