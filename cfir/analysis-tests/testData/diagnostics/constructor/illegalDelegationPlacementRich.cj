// RUN_PIPELINE_TILL: FRONTEND
open class Base {
    init(v: Int64) {
        let _ = v
    }
}

class IllegalThisInMember {
    func build(): Unit {
        <!ILLEGAL_THIS_OR_SUPER_CALL!>this<!>()
    }
}

class IllegalSuperInMember <: Base {
    init() {
        super(1)
    }

    func rebuild(): Unit {
        <!ILLEGAL_THIS_OR_SUPER_CALL!>super<!>(1)
    }
}
