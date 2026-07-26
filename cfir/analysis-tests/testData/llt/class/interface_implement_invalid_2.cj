


open class Base {}

class Derived <: Base {}

interface I1 {
    func foo(): Base
}

abstract class C1 <: I1 {}

class <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>C2<!> <: I1 {}

class C3 <: I1 {
    public func <!RETURN_TYPE_INCOMPATIBLE!>foo<!>(): Int64 {
        return 0
    }
}

class C4 <: <!UNDECLARED_TYPE_NAME!>I2<!> {
    public func foo(): Base {
        return Base()
    }
}

class C5 <: <!UNDECLARED_TYPE_NAME!>I2<!> {
    public func foo(): Derived {
        return Derived()
    }
}

main(): Int64 {
    return 0
}
