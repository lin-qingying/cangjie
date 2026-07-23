

struct B {
    public var a: Int32 = 1
    public let b: Int32 = 1
}

main(): Int64 {
    A().a = 2
    A().<!CANNOT_ASSIGN_TO_IMMUTABLE!>b<!> = 2
    B().<!CANNOT_ASSIGN_TO_IMMUTABLE!>a<!> = 2
    B().<!CANNOT_ASSIGN_TO_IMMUTABLE!>b<!> = 2
    return 0
}

class A {
    public var a: Int32 = 1
    public let b: Int32 = 1
}
