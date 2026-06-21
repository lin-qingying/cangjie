


interface I1 {
    mut static prop a: Int64 {
        get() {
            0
        }
        set(v) {}
    }
    static func f(): Unit {}
}

interface I2 <: I1 {
    <!INVALID_OVERRIDE_OR_REDEFINE_MEMBER_IN_INTERFACE!>m<!>ut static prop a: Int64
    <!INVALID_OVERRIDE_OR_REDEFINE_MEMBER_IN_INTERFACE!>s<!>tatic func f(): Unit
}

main() {}
