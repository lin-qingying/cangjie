// RUN_PIPELINE_TILL: FRONTEND
open class Base {
    public open func value(): Int64 {
        return 1
    }
}

class Child <: Base {
    public override func value(): Int64 {
        return super.value()
    }
}

func readValue(c: Child): Int64 {
    return c.value()
}
