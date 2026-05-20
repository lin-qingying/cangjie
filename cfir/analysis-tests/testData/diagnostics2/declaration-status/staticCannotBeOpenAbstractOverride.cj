// RUN_PIPELINE_TILL: FRONTEND
open class Parent {
    public open func canOverride(): Unit {}
}

class Child <: Parent {
    public <!INCOMPATIBLE_MODIFIERS!>static<!> <!INCOMPATIBLE_MODIFIERS!>override<!> func canOverride(): Unit {}
}

class StaticStatus {
    public <!INCOMPATIBLE_MODIFIERS!>static<!> <!INCOMPATIBLE_MODIFIERS!>open<!> func openFn(): Unit {}
    public <!INCOMPATIBLE_MODIFIERS!>static<!> <!INCOMPATIBLE_MODIFIERS!>operator<!> func +(value: Int64): Int64 { value }
    <!REDEF_INSTANCE_ERROR!>redef<!> func badRedef(): Unit {}
}

abstract class AbstractStaticStatus {
    static <!WRONG_MODIFIER_TARGET!>abstract<!> func absFn(): Unit
}
