// RUN_PIPELINE_TILL: FRONTEND
open class Base {
    init(value: Int64) {
        let _ = value
    }
}

class MissingSuper <: Base {
    <!EXPLICIT_SUPER_CALL_REQUIRED!>init<!>() {
    }
}
