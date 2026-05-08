// RUN_PIPELINE_TILL: FRONTEND
open class Parent {
    public open func canOverride(): Unit {}
}

class Child <: Parent {
    public <!INCOMPATIBLE_MODIFIERS!>static<!> <!INCOMPATIBLE_MODIFIERS!>override<!> func <!STATIC_CANNOT_BE_OPEN_ABSTRACT_OVERRIDE!>canOverride<!>(): Unit {}
}

class StaticStatus {
    public <!INCOMPATIBLE_MODIFIERS!>static<!> <!INCOMPATIBLE_MODIFIERS!>open<!> func <!STATIC_CANNOT_BE_OPEN_ABSTRACT_OVERRIDE!>openFn<!>(): Unit {}
    public <!INCOMPATIBLE_MODIFIERS!>static<!> <!INCOMPATIBLE_MODIFIERS!>operator<!> func <!STATIC_CANNOT_BE_OPEN_ABSTRACT_OVERRIDE!>+<!>(value: Int64): Int64 { value }
    <!REDEF_INSTANCE_ERROR!>redef<!> func badRedef(): Unit {}
}

abstract class AbstractStaticStatus {
    static <!WRONG_MODIFIER_TARGET!>abstract<!> func <!STATIC_CANNOT_BE_OPEN_ABSTRACT_OVERRIDE!>absFn<!>(): Unit
}
