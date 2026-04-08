// RUN_PIPELINE_TILL: FRONTEND
open class Base {}
class Derived <: Base {}

open class Parent {
    open func value(): Derived {
        return Derived()
    }
}

class Child <: Parent {
    <!OVERRIDING_RETURN_TYPE_MISMATCH!>override func value(): Base {
        return Base()
    }<!>
}
