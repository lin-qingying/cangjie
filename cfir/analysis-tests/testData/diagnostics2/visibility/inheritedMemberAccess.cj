// RUN_PIPELINE_TILL: FRONTEND
open class Base {
    func greet(): Int64 {
        return 1
    }
}

class Child <: Base {}

func useChild(c: Child): Int64 {
    return c.greet()
}
