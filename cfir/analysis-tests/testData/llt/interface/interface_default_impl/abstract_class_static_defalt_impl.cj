

interface I {
    static func f1(): Unit
}

abstract class <!INHERIT_ABSTRACT_CLASS_STATIC_UNIMPLEMENT_FUNC!>AbsC1<!> <: I {}

main() {
    AbsC1.<!INTERFACE_CALL_WITH_UNIMPLEMENTED_CALL!>f1<!>()
}
