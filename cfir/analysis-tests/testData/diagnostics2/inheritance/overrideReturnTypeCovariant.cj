// RUN_PIPELINE_TILL: FRONTEND
open class Base {
    open func getValue(): Base {
        return Base()
    }
}

class Child <: Base {
    func getValue(): Child {
        return Child()
    }
}
