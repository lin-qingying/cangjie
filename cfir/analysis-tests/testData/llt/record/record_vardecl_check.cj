

struct A {
    public var b: Int64 = 1
    public let c: B = <!RECURSIVE_CONSTRUCTOR_CALL!>B()<!>
}

class B {
    public let c: A = <!RECURSIVE_CONSTRUCTOR_CALL!>A()<!>
    public var d: Int64 = 1
}

func m(b: A) {
    b.<!CANNOT_ASSIGN_TO_IMMUTABLE!>b<!> = 1
}

class FooArray {
    public init() {}
    public operator func [](it: FooArray, index: Int64): A {
        return A()
    }
}

type F = A

main() {
    let a = B()
    a.c.<!CANNOT_ASSIGN_TO_IMMUTABLE!>b<!> = 1
    let b = A()
    b.<!CANNOT_ASSIGN_TO_IMMUTABLE!>b<!> = 1
    b.c.d = 3 // OK
    let arr = FooArray()
    <!INVALID_SUBSCRIPT_EXPR!>arr[0]<!>.b = 4
    let f: F = A()
    f.<!CANNOT_ASSIGN_TO_IMMUTABLE!>b<!> = 1
    return 0
}
