// RUN_PIPELINE_TILL: FRONTEND
open class Base {
    open func value(): Int64 {
        return 1
    }
}

class Child <: Base {
    override func value(): Int64 {
        return super.value()
    }
}

func readValue(c: Child): Int64 {
    return c.value()
}
