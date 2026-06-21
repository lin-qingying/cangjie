


open class A {
    public open func f(): String {
        return ""
    }
    public open prop a: Int {
        get() {
            0
        }
    }
}

abstract class B <: A {
    <!INVALID_OVERRIDE_MEMBER_IN_CLASS!>p<!>ublic func f(): String
    <!INVALID_OVERRIDE_MEMBER_IN_CLASS!>p<!>ublic prop a: Int
}

main() {}
