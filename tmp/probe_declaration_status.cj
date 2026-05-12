open class Parent {
    public open func canOverride(): Unit {}
}

class Child <: Parent {
    public static override func canOverride(): Unit {}
}

class StaticStatus {
    public static open func openFn(): Unit {}
    public static operator func call(): Unit {}
}

abstract class AbstractStaticStatus {
    static abstract func absFn(): Unit
}

open interface Marker {
    private func hidden(): Unit {}
    internal func shared(): Unit {}
}

abstract open class AbstractOpen {}

sealed public class PublicSealed {}

sealed open class OpenSealed {}
