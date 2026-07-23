// RUN_PIPELINE_TILL: FRONTEND
class ThisOnly {
    init(value: Int64, again: Int64) {
        let _ = value
        let _ = again
    }

    init() {
        this<!WRONG_NUMBER_OF_ARGUMENTS!>(1)<!>
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
        super<!WRONG_NUMBER_OF_ARGUMENTS!>(1)<!>
    }
}

interface Factory {}

func directConstruction(): Unit {
    let _ = <!NO_CONSTRUCTOR!>Factory<!>()
}
