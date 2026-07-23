

struct A {
    public var a: Int32 = 1
    public let b: Int32 = 1
}

main(): Int64 {
    var x = A()
    x.a = 2
    x.<!CANNOT_ASSIGN_TO_IMMUTABLE!>b<!> = 2

    let y = A()
    y.<!CANNOT_ASSIGN_TO_IMMUTABLE!>a<!> = 2
    y.<!CANNOT_ASSIGN_TO_IMMUTABLE!>b<!> = 2

    var m = B()
    m.a = 2
    m.<!CANNOT_ASSIGN_TO_IMMUTABLE!>b<!> = 2

    let n = B()
    n.<!CANNOT_ASSIGN_TO_IMMUTABLE!>a<!> = 2
    n.<!CANNOT_ASSIGN_TO_IMMUTABLE!>b<!> = 2

    return 0
}

struct B {
    public var a: Int32 = 1
    public let b: Int32 = 1
}
