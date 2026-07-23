

main(): Int64 {
    return 0
}

class B <: A {
    public init() {
        a = 0
        b = 0
    }

    public func foo1() {
        a = 0
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>b<!> = 0
    }

    public func foo2() {
        this.a = 0
        this.<!CANNOT_ASSIGN_TO_IMMUTABLE!>b<!> = 0
    }
}

open class A {
    public var a: Int32 = 1
    public let <!CLASS_UNINITIALIZED_FIELD!>b<!>: Int32
}
