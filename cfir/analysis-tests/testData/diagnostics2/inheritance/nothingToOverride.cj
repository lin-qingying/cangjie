// RUN_PIPELINE_TILL: FRONTEND
open class Parent {
    open func present(): Int64 {
        return 1
    }
}

class Child <: Parent {
    <!NOTHING_TO_OVERRIDE!>override<!> func missing(): Int64 {
        return 0
    }
}
