

interface Base {
    func f1(): Int32
    func f2(): Int32 {
        return f1()
    }
}

class Data <: Base {
    public var m: Int32
    public init() {
        m = <!USED_BEFORE_INITIALIZATION!>f2<!>()
    }

    public func f1(): Int32 {
        return m
    }
}
