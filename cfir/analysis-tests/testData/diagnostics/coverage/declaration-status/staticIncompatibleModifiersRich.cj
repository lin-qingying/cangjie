// RUN_PIPELINE_TILL: FRONTEND
open class Parent {
    open func canOverride(): Unit {}
}

class Child <: Parent {
    <!INCOMPATIBLE_MODIFIERS!>static<!> <!INCOMPATIBLE_MODIFIERS, OVERRIDE_STATIC_ERROR!>override<!> func canOverride(): Unit {}
}

class StaticStatus {
    <!INCOMPATIBLE_MODIFIERS!>static<!> <!INCOMPATIBLE_MODIFIERS!>open<!> func openFn(): Unit {}
    <!INCOMPATIBLE_MODIFIERS!>static<!> <!INCOMPATIBLE_MODIFIERS!>operator<!> func call(): Unit {}
    <!REDEF_INSTANCE_ERROR!>redef<!> func badRedef(): Unit {}
}

abstract class AbstractStaticStatus {
    static <!WRONG_MODIFIER_TARGET!>abstract<!> func absFn(): Unit
}
