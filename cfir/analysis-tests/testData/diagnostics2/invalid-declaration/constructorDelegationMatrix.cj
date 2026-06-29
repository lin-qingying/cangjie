// RUN_PIPELINE_TILL: FRONTEND
open class Base {
    init(value: Int64) {
        let _ = value
    }
}

class MissingSuper <: Base {
    <!NO_NON_PARAM_CONSTRUCTOR_IN_SUPER_CLASS!>init<!>() {
    }
}

class Recursive {
    <!RECURSIVE_CONSTRUCTOR_CALL!>init<!>() {
        this()
    }
}
