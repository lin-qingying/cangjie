// RUN_PIPELINE_TILL: FRONTEND
open class Base {
    init(v: Int64) {}
}

class IllegalSuperInMember <: Base {
    init() {
        super(1)
    }

    func rebuild(): Unit {
        <!ILLEGAL_THIS_OR_SUPER_CALL!>super<!>(1)
    }
}
