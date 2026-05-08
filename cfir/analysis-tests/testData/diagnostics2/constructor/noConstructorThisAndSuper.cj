// RUN_PIPELINE_TILL: FRONTEND
class ThisOnly {
    init(value: Int64, again: Int64) {
        let _ = value
        let _ = again
    }

    init() {
        this(<!TOO_MANY_ARGUMENTS!>1<!>)
    }
}

open class Base {
    init(value: Int64, again: Int64) {
        let _ = value
        let _ = again
    }
}

class Child <: Base {
    init() {
        super(<!NO_VALUE_FOR_PARAMETER!>1<!>)
    }
}
