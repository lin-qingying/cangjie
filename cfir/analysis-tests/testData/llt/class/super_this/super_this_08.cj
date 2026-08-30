

interface Base {
    func f1(): Int32
    func f2(): Int32 {
        return f1()
    }
}

class Data <: Base {
    public var m: Int32
    public init() {
        m = <!ILLEGAL_USAGE_OF_MEMBER!>f2<!>()
    }

    public func f1(): Int32 {
        return m
    }
}
