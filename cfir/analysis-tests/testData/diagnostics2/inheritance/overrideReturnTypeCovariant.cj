// RUN_PIPELINE_TILL: FRONTEND
open class Base {
    public open func getValue(): Base {
        return Base()
    }
}

class Child <: Base {
    public override func getValue(): Child {
        return Child()
    }
}
