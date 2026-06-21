

open class Father {}

class Child <: Father {}

interface I1 {
    func f(a: Int32): Father
}

interface I2 {
    func f(a: Int32): Child
}

class C <: I1 & I2 {
    public func <!OVERRIDING_RETURN_TYPE_MISMATCH!>f<!>(a: Int32): Father {
        Father()
    }
}
