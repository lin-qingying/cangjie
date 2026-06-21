


interface I {
    static func f2(a!: Int64, b!: Int64): Int64
}

class <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>A<!> <: I {}

interface I2 <: I {}

class B {
    static func <!STATIC_CANNOT_BE_OPEN_ABSTRACT_OVERRIDE!>a<!>(): Unit
}
