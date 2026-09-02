


interface I {
    static func f2(a!: Int64, b!: Int64): Int64
}

class <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>A<!> <: I {}

interface I2 <: I {}

class <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>B<!> {
    <!MISSING_FUNC_BODY!>s<!>tatic func a(): Unit
}
