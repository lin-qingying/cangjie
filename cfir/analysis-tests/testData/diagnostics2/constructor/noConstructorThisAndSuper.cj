// RUN_PIPELINE_TILL: FRONTEND
class ThisOnly {
    init(value: Int64, again: Int64) {}

    init() {
        <!NO_CONSTRUCTOR!>this<!>(1)
    }
}

open class Base {
    init(value: Int64, again: Int64) {}
}

class Child <: Base {
    init() {
        <!NO_CONSTRUCTOR!>super<!>(1)
    }
}
