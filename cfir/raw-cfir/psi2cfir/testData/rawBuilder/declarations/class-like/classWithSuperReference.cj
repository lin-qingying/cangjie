// RUN_PIPELINE_TILL: FRONTEND
open class Base {
    open func value(): Int64 {
        return 1
    }
}

class Box <: Base {
    override func value(): Int64 {
        return super.value()
    }
}
