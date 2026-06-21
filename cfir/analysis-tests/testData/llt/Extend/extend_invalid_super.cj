


open class A {
    public let x: Int64 = 1
}

class B <: A {}

extend B {
    public func foo(): Int64 {
        return <!EXTEND_SUPER_NOT_ALLOWED!>super<!>.x
    }
}
