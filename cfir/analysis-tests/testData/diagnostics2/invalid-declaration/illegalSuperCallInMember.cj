// RUN_PIPELINE_TILL: FRONTEND
open class Base {
    init(v: Int64) {}
}

class IllegalSuperInMember <: Base {
    init() {
        super(1)
    }

    func rebuild(): Unit {
        <!INVALID_THIS_CALL_OUTSIDE_CTOR!>super<!>(1)
    }
}
