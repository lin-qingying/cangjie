// RUN_PIPELINE_TILL: FRONTEND
open class Parent {
    open func canOverride(): Unit {}
}

class Child <: Parent {
    <!STATIC_CANNOT_BE_OPEN_ABSTRACT_OVERRIDE!><!INCOMPATIBLE_MODIFIERS!>static<!> <!INCOMPATIBLE_MODIFIERS, OVERRIDE_STATIC_ERROR!>override<!> func canOverride(): Unit {}<!>
}

class StaticStatus {
    <!STATIC_CANNOT_BE_OPEN_ABSTRACT_OVERRIDE!><!INCOMPATIBLE_MODIFIERS!>static<!> <!INCOMPATIBLE_MODIFIERS!>open<!> func openFn(): Unit {}<!>
    <!INCOMPATIBLE_MODIFIERS!>static<!> <!INCOMPATIBLE_MODIFIERS!>operator<!> func call(): Unit {}
    <!REDEF_INSTANCE_ERROR!>redef<!> func badRedef(): Unit {}
}

abstract class AbstractStaticStatus {
    <!STATIC_CANNOT_BE_OPEN_ABSTRACT_OVERRIDE!>static <!WRONG_MODIFIER_TARGET!>abstract<!> func absFn(): Unit<!>
}
