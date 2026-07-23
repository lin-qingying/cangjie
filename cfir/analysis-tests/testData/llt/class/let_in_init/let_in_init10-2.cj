

main(): Int64 {
    return 0
}

class C <: A {
    public init() {
        a = 0
        b = 0
    }
    public init(c: Int32) {
        a = 0
        b = 0
    }
}

open class A {
    public var a: Int32 = 1
    public let <!CLASS_UNINITIALIZED_FIELD!>b<!>: Int32
}

class D <: E {
    public init() {
        a = 0
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>b<!> = 0
    }
    public init(c: Int32) {
        a = 0
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>b<!> = 0
    }
}

open class E {
    public var a: Int32 = 1
    public let b: Int32 = 1
}
