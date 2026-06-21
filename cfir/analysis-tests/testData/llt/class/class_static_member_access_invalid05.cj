

interface I {
    static func test(): Unit
}

abstract class <!INHERIT_ABSTRACT_CLASS_STATIC_UNIMPLEMENT_FUNC!>A<!> <: I {}

type B = A

main(): Int64 {
    B.<!INTERFACE_CALL_WITH_UNIMPLEMENTED_CALL!>test<!>()
    return 0
}
