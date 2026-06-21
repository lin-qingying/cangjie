

struct B {
    public var a: Int32 = 1
    public let b: Int32 = 1
    public var c: D = D()
}

struct D {
    public var c: A = A()
}

main(): Int64 {
    A().a = 2
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>A().b<!> = 2
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>B().a<!> = 2
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>B().b<!> = 2

    return 0
}

class A {
    public var a: Int32 = 1
    public let b: Int32 = 1
}
