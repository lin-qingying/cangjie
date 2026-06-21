


class A {
    public func f(): Int64 {
        0
    }
}

extend A {
    prop <!EXTEND_MEMBER_CANNOT_SHADOW!>f<!>: String {
        get() {
            return "C"
        }
    }
}

class B {
    public var f: String = "2"
}

extend B {
    prop <!EXTEND_MEMBER_CANNOT_SHADOW!>f<!>: String {
        get() {
            return "C"
        }
    }
}

extend B {
    prop <!EXTEND_MEMBER_CANNOT_SHADOW!>f<!>: String {
        get() {
            return "C"
        }
    }
}
