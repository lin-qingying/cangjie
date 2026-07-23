

struct B {
    public var a: Int32 = 1
    public let b: Int32 = 1
}

func m() {
    return B()
}

func k() {
    return A()
}

main(): Int64 {
    k().a = 2
    k().<!CANNOT_ASSIGN_TO_IMMUTABLE!>b<!> = 2
    m().<!CANNOT_ASSIGN_TO_IMMUTABLE!>a<!> = 2
    m().<!CANNOT_ASSIGN_TO_IMMUTABLE!>b<!> = 2

    return 0
}

class A {
    public var a: Int32 = 1
    public let b: Int32 = 1
}
