

abstract class C1 {
    public var a: Int32 = 5
    public func f(): Int32
}

open class C2 <: C1 {
    public func f(): Int32 {
        return 5
    }
}

class C3 <: C2 {
    public var k: Int32
    public init() {
        k = super.<!ILLEGAL_USAGE_OF_MEMBER!>f<!>()
    }
    public func getK(): Int32 {
        return k
    }
}

main() {
    var ins = C3()
    if (ins.getK() == 5) {
        return 0
    }
    return 1
}
