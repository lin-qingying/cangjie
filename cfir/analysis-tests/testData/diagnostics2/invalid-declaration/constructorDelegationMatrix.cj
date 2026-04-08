// RUN_PIPELINE_TILL: FRONTEND
open class Base {
    init(value: Int64) {}
}

class MissingSuper <: Base {
    <!EXPLICIT_SUPER_CALL_REQUIRED!>init() {
    }<!>
}

class Recursive {
    init() {
        <!RECURSIVE_CONSTRUCTOR_CALL!>this<!>()
    }
}
