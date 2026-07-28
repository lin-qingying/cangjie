


abstract class FaTest {
    public var a: Int32 = 5
    public func f(): Int32
}

class Test1 <: FaTest {
    public var fPtr: () -> Int32
    public init(x: Int32) {
        a = x
        fPtr = <!ABSTRACT_METHOD_CANNOT_BE_ACCESSED_DIRECTLY!>s<!>uper.<!USED_BEFORE_INITIALIZATION!>f<!>
    }
    public func f(): Int32 {
        return a
    }
    public func get(): Int32 {
        return fPtr()
    }
}

main() {
    var ins = Test1(15)
    print("${ins.get()}\n")
    return 0
}
