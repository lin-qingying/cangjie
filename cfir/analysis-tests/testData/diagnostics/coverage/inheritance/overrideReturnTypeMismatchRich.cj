// RUN_PIPELINE_TILL: FRONTEND
open class Base {}
class Derived <: Base {}

open class Parent {
    public open func value(): Derived {
        return Derived()
    }
}

class Child <: Parent {
    public override func <!OVERRIDING_RETURN_TYPE_MISMATCH!>value<!>(): Base {
        return Base()
    }
}
