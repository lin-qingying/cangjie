


class C <: A {
    public init() {
        a = 0
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>b = 0<!>
    }
    public init(c: Int32) {
        a = 0
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>b = 0<!>
    }
}

open class A {
    public mut prop a: Int64 {
        get() {
            return 0
        }
        set(v) {}
    }
    public prop b: Int64 {
        get() {
            return 0
        }
    }
    public init() {
        <!CANNOT_ASSIGN_TO_IMMUTABLE!><!ILLEGAL_MEMBER_USED_IN_OPEN_CONSTRUCTOR!>b<!> = 1<!>
    }
}

main(): Int64 {
    return 0
}
