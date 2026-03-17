open class Parent {
    open func canOverride(): Unit {}
}

class Child <: Parent {
    <!STATIC_CANNOT_BE_OPEN_ABSTRACT_OVERRIDE!>static override func canOverride(): Unit {}<!>
}

class StaticStatus {
    <!STATIC_CANNOT_BE_OPEN_ABSTRACT_OVERRIDE!>static open func openFn(): Unit {}<!>
}

abstract class AbstractStaticStatus {
    <!STATIC_CANNOT_BE_OPEN_ABSTRACT_OVERRIDE!>static abstract func absFn(): Unit<!>
}
