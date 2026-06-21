


abstract class FaTest {
    public var a: Int32 = 1
    public func f(): Int32
}

class Test1 <: FaTest {
    public init(x: Int32) {
        a = <!ABSTRACT_METHOD_CANNOT_BE_ACCESSED_DIRECTLY!>s<!>uper.f()
    }
    public func f(): Int32 {
        return 5
    }
}

main() {
    return 0
}
