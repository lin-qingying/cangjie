

abstract class C {
    public func f(): Unit
}

interface I {
    func f(): Unit
}

extend C <: I {
    public func <!EXTEND_MEMBER_CANNOT_SHADOW!>f<!>(): Unit {} // redefinition
}
