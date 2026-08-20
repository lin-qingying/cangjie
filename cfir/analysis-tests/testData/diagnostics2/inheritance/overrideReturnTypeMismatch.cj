// RUN_PIPELINE_TILL: FRONTEND
open class Base {}
class Derived <: Base {}

open class Parent {
    public open func value(): Derived {
        return Derived()
    }
}

class Child <: Parent {
    public override func <!RETURN_TYPE_INCOMPATIBLE!>value<!>(): Base {
        return Base()
    }
}
