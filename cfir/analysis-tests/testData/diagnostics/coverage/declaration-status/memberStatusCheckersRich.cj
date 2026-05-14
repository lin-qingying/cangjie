// RUN_PIPELINE_TILL: FRONTEND
open class Parent {
    public open func canOverride(): Unit {}
}

interface MutCarrier {
    mut func denied(): Unit
}

struct Counter {
    mut func allowed(): Unit {}
}

class StaticStatus {
    public <!INCOMPATIBLE_MODIFIERS!>static<!> <!INCOMPATIBLE_MODIFIERS!>open<!> func openFn(): Unit {}
}

class StaticOverride <: Parent {
    public <!INCOMPATIBLE_MODIFIERS!>static<!> <!INCOMPATIBLE_MODIFIERS!>override<!> func canOverride(): Unit {}
}

abstract class AbstractStaticStatus {
    static <!WRONG_MODIFIER_TARGET!>abstract<!> func absFn(): Unit
}
